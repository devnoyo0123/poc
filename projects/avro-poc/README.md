# Spring Cloud Bus POC - Kafka 기반 설정 동기화

Spring Cloud Bus 패턴을 사용하여 Kafka를 통해 분산 환경의 서버들에 설정 변경을 실시간으로 전파하는 POC 프로젝트

## 🎯 목적

**Spring Cloud Bus의 핵심 개념 증명:**
- ✅ **진짜 Spring Environment Property** 동적 변경
- ✅ **@RefreshScope** Bean 재생성을 통한 설정 갱신
- ✅ **Kafka 메시지**를 통한 실시간 전파
- ✅ **다중 서버** 간 설정 동기화

> **참고:** 이 POC는 Spring Config Server 없이 **Kafka + ContextRefresher**만으로 구현

---

## 🏗️ 아키텍처

### 실행 구조

```
┌─────────────────┐         ┌─────────────────┐
│   Server 1      │         │   Server 2      │
│   Port 8088      │         │   Port 8089      │
│                 │         │                 │
│  ┌───────────┐  │         │  ┌───────────┐  │
│  │Producer   │  │         │  │Producer   │  │
│  └─────┬─────┘  │         │  └─────┬─────┘  │
└────────┼────────┘         └────────┼────────┘
         │                           │
         └──────────┬────────────────┘
                      ↓
              ┌──────────────────┐
              │   Kafka Topic    │
              │  config-events   │
              └───────┬───────────┘
                      │
           ┌──────────┴───────────┐
           ↓                      ↓
      ┌───────────────────────────────┐
      │  Server 1 & 2 Consumer       │
      │  (둘 다 Kafka 메시지 받음)    │
      │  각자 다른 consumer group    │
      └───────────────────────────────┘
```

### 설정 전파 흐름

```
1. API 호출 (POST /api/config/feature-flag)
   ↓
2. Kafka Producer가 Avro 메시지 발행
   ↓
3. 두 서버의 Consumer가 메시지 수신
   ↓
4. Environment에 Property 추가 (MapPropertySource)
   ↓
5. ContextRefresher.refresh() 호출
   ↓
6. @RefreshScope Bean 재생성 → 새 @Value 로드
   ↓
7. ✅ 진짜 Spring Environment Property 변경 완료
```

---

## 🚀 실행 방법

### 1. 인프라 시작

```bash
# Docker Compose로 Kafka, Schema Registry, Kafka UI 시작
docker-compose up -d

# 확인
docker-compose ps
```

### 2. 단일 서버 실행

```bash
# Server 1 (Port 8088)
export APP_ENV=local
./gradlew bootRun
```

### 3. 다중 서버 실행 (POC 권장)

```bash
# 두 서버 동시 시작
./run-multi.sh

# 또는 수동으로:
# Terminal 1
SERVER_PORT=8088 ./gradlew bootRun

# Terminal 2
SERVER_PORT=8089 ./gradlew bootRun
```

---

## 🧪 테스트 방법

### 1. 현재 설정 확인

```bash
# In-memory Map (이전 방식)
curl http://localhost:8088/api/config | jq .

# @RefreshScope Bean (진짜 Spring Property) ⭐
curl http://localhost:8088/api/config/refreshed | jq .
```

### 2. Feature Flag 업데이트

```bash
# new-ui = true로 변경
curl -X POST http://localhost:8088/api/config/feature-flag \
  -H "Content-Type: application/json" \
  -d '{
    "key": "new-ui",
    "value": true
  }' | jq .
```

**응답:**
```json
{
  "success": true,
  "message": "Feature flag updated and event published",
  "configType": "featureFlag",
  "key": "new-ui",
  "value": true
}
```

### 3. 전파 확인

```bash
# 8088 서버 확인
curl http://localhost:8088/api/config/refreshed | jq '.featureFlags["new-ui"]'

# 8089 서버 확인
curl http://localhost:8089/api/config/refreshed | jq '.featureFlags["new-ui"]'

# ⭐ 둘 다 true여야 성공!
```

---

## ✅ 검증 방법

### 1. @RefreshScope Bean 값 확인

```bash
# 전: new-ui = false
curl http://localhost:8088/api/config/refreshed | jq '.featureFlags["new-ui"]'
# false

# 업데이트 후
curl -X POST http://localhost:8088/api/config/feature-flag \
  -H "Content-Type: application/json" \
  -d '{"key": "new-ui", "value": true}'

# 확인
curl http://localhost:8088/api/config/refreshed | jq '.featureFlags["new-ui"]'
# true ✅
```

### 2. Actuator로 진짜 Environment Property 확인

```bash
# 특정 Property의 소스 확인
curl http://localhost:8088/actuator/env/app.feature-flags.new-ui | jq '.property.source'

# 응답: "kafka-config-events" ⭐
# 이게 보이면 진짜 Environment Property가 변경된 것!
```

### 3. 다중 서버 동기화 검증

```bash
# 업데이트 전
curl http://localhost:8088/api/config/refreshed | jq '.featureFlags["new-ui"]'  # false
curl http://localhost:8089/api/config/refreshed | jq '.featureFlags["new-ui"]'  # false

# 업데이트
curl -X POST http://localhost:8088/api/config/feature-flag \
  -H "Content-Type: application/json" \
  -d '{"key": "new-ui", "value": true}'

# 3초 후 확인
sleep 3
curl http://localhost:8088/api/config/refreshed | jq '.featureFlags["new-ui"]'  # true ✅
curl http://localhost:8089/api/config/refreshed | jq '.featureFlags["new-ui"]'  # true ✅
```

### 4. 로그 확인

```bash
# 로그에서 다음 메시지 확인:
tail -f server1.log | grep -E "(Property added|Context refreshed|FeatureFlag updated)"

# 예상 출력:
# ✅ Property added to Environment
# 🔄 Refreshing Spring context...
# ✅ Context refreshed! Updated keys: []
# ✅ FeatureFlag updated: new-ui = true
```

---

## 🔧 핵심 구현

### 1. @RefreshScope Bean

```kotlin
@Service
@RefreshScope  // ⬅️ 핵심!
class FeatureFlagService {

    @Value("\${app.feature-flags.new-ui:false}")
    var newUiEnabled: Boolean = false  // Context refresh 시 재로드

    fun isNewUiEnabled(): Boolean = newUiEnabled
}
```

### 2. Environment Refresh (Consumer)

```kotlin
@KafkaListener(topics = ["config-events"], groupId = "config-consumer-group-${serverPort}")
fun consumeConfigEvent(message: GenericRecord) {
    val event = toConfigChangeEvent(message)

    // 1. Environment에 Property 직접 추가
    val propertyName = "app.feature-flags.${event.key}"
    val propertySource = MapPropertySource(
        "kafka-config-events",
        mapOf(propertyName to event.value.toString())
    )
    environment.propertySources.addFirst(propertySource)  // 우선순위 최상위!

    // 2. Context refresh
    val refreshedKeys = contextRefresher.refresh()  // ⬅️ 핵심!

    // 3. @RefreshScope Bean이 새 값을 읽음
}
```

### 3. Kafka Consumer Group 전략

```kotlin
// 각 서버가 다른 groupId 사용 → 모두가 모든 메시지 수신
@KafkaListener(
    topics = ["config-events"],
    groupId = "config-consumer-group-${server.port}"  // 8088, 8089가 다른 그룹
)
```

**중요:** 같은 파티션을 여러 Consumer가 받으려면 **각자 다른 groupId**를 사용해야 합니다!

---

## 📂 프로젝트 구조

```
avro-poc/
├── src/main/kotlin/com/example/avro/
│   ├── consumer/
│   │   └── ConfigEventConsumer.kt          # Kafka 메시지 수신 + Environment refresh
│   ├── controller/
│   │   └── ConfigController.kt             # API endpoint
│   ├── service/
│   │   └── FeatureFlagService.kt           # @RefreshScope Bean
│   ├── config/
│   │   ├── AppConfig.kt                     # In-memory Map (이전 방식)
│   │   └── KafkaConsumerConfig.kt           # Kafka 설정
│   └── producer/
│       └── UserEventProducer.kt            # Avro 메시지 발행
├── src/main/resources/
│   ├── application.yml                      # 기본 설정
│   ├── application-local.yml                # 로컬 설정
│   └── avro/
│       └── ConfigChangeEvent.avsc            # Avro Schema
├── docker-compose.yml                        # Kafka, Schema Registry
├── run-multi.sh                              # 다중 서버 실행 스크립트
└── build.gradle.kts                          # Spring Cloud 의존성 포함
```

---

## 📊 기술 스택

| 기술 | 용도 | 버전 |
|------|------|------|
| **Spring Boot** | 애플리케이션 | 3.2.0 |
| **Spring Cloud** | @RefreshScope, ContextRefresher | 2023.0.0 |
| **Spring Kafka** | Kafka 통합 | - |
| **Kafka** | 메시지 브로커 | 3.6.0 |
| **Confluent Schema Registry** | Avro Schema 관리 | 7.5.0 |
| **Actuator** | 환경변수 검증 | - |

---

## 🎯 핵심 기능

### ✅ 구현된 기능

- [x] Kafka 기반 설정 전파 (Spring Cloud Bus 패턴)
- [x] @RefreshScope Bean 재생성
- [x] ContextRefresher를 통한 Environment refresh
- [x] **진짜 Spring Property** 동적 변경
- [x] 다중 서버 간 설정 동기화
- [x] Avro 메시지 포맷
- [x] Actuator를 통한 검증

### 🔍 비교: 이전 vs 현재

| 구분 | 이전 구현 | 현재 구현 |
|------|---------|---------|
| 저장소 | In-memory Map | Spring Environment |
| 읽기 방식 | `appConfig.getMap()` | `@Value` annotation |
| 변경 방식 | `Map.put()` | `Environment + refresh()` |
| Bean 재로드 | ❌ 없음 | ✅ @RefreshScope |
| 진짜 Property | ❌ 아니요 | ✅ 네 |
| 검증 방법 | `/api/config` | `/api/config/refreshed`, `/actuator/env` |

---

## 🐛 Troubleshooting

### 문제: 한쪽 서버만 메시지를 받음

**원인:** 두 서버가 같은 `groupId`를 사용해서 파티션 경쟁

**해결:** 각 서버가 다른 groupId 사용
```kotlin
groupId = "config-consumer-group-${server.port}"
```

### 문제: 값이 항상 false로 업데이트됨

**원인:** Avro `Utf8` 타입이 `is String` 체크에서 실패

**해결:** 타입 체크에 `else` 블록 추가
```kotlin
val value = when (event.value) {
    is String -> (event.value as String).toBoolean()
    is Boolean -> event.value as Boolean
    else -> event.value.toString().toBoolean()  // ⬅️ 추가
}
```

### 문제: ContextRefresher가 변경을 감지 못함

**증상:** `Updated keys: []`

**원인:** 기존 Property를 수정한 게 아니라 **새 PropertySource 추가**

**정상 동작:** @RefreshScope Bean은 Environment의 모든 PropertySources를 검색해서 맨 앞의 `kafka-config-events`에서 값을 읽음

---

## 🔗 참고 자료

- [Spring Cloud Bus](https://docs.spring.io/spring-cloud-bus/reference/html/)
- [Spring Cloud Context: Refresh Scope](https://docs.spring.io/spring-cloud-framework/reference/cloud/context.html)
- [ContextRefresher Javadoc](https://docs.spring.io/spring-cloud-framework/docs/api/org/springframework/cloud/context/refresh/ContextRefresher.html)
- [Spring Kafka](https://docs.spring.io/spring-kafka/reference/)

---

## 📝 License

MIT
