# Redis Serializer POC

> `StringRedisSerializer` vs `GenericJackson2JsonRedisSerializer` 실제 저장 형식 비교
>
> Multi-module Gradle (Kotlin + JDK 21 + Spring Boot 3.3)

## 왜 만들었나

두 Serializer의 차이를 **redis-cli로 직접 확인**하기 위함.
같은 User 객체를 각각 다른 Serializer로 저장 → Redis CLI로 원시 바이트 비교 → `@class` 필드/용량 차이 체감.

## 구조

```
redis-serializer-poc/
├── settings.gradle.kts          # multi-module
├── build.gradle.kts             # 공통 Kotlin/JDK21 설정
├── docker-compose.yml           # 공유 Redis 7 (단일 인스턴스)
├── string-serializer/           # 모듈 1 (port 8080)
│   ├── build.gradle.kts
│   └── src/main/kotlin/.../stringser/
│       ├── config/RedisConfig.kt       # StringRedisSerializer
│       ├── domain/User.kt
│       ├── service/UserCacheService.kt # 수동 ObjectMapper
│       └── controller/UserController.kt
└── jackson-serializer/          # 모듈 2 (port 8081)
    ├── build.gradle.kts
    └── src/main/kotlin/.../jacksonser/
        ├── config/RedisConfig.kt       # GenericJackson2JsonRedisSerializer
        ├── domain/User.kt
        ├── service/UserCacheService.kt # 객체 그대로 저장
        └── controller/UserController.kt
```

## 실행

### 1. Redis 기동 (공유)

```bash
docker compose up -d
```

### 2. 두 서버 동시 실행 (각각 다른 터미널)

```bash
# 터미널 1 — String Serializer
./gradlew :string-serializer:bootRun
# → port 8080

# 터미널 2 — Jackson Serializer
./gradlew :jackson-serializer:bootRun
# → port 8081
```

### 3. 동일 User 저장 (양쪽)

```bash
# String 쪽
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"id":"1","name":"alice","email":"alice@example.com","age":30}'

# Jackson 쪽
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"id":"1","name":"alice","email":"alice@example.com","age":30}'
```

### 4. Redis CLI로 저장 형식 직접 비교 (핵심)

```bash
docker exec -it serializer-poc-redis redis-cli

# StringRedisSerializer — 깨끗한 JSON
127.0.0.1:6379> GET string:user:1
"{\"id\":\"1\",\"name\":\"alice\",\"email\":\"alice@example.com\",\"age\":30}"
127.0.0.1:6379> STRLEN string:user:1
(integer) 62

# GenericJackson2JsonRedisSerializer — @class 필드 포함
127.0.0.1:6379> GET jackson:user:1
"{\"@class\":\"com.example.jacksonser.domain.User\",\"id\":\"1\",\"name\":\"alice\",\"email\":\"alice@example.com\",\"age\":30}"
127.0.0.1:6379> STRLEN jackson:user:1
(integer) 108
```

## 핵심 차이 정리

| 구분 | StringRedisSerializer | GenericJackson2JsonRedisSerializer |
|------|----------------------|------------------------------------|
| **저장 형식** | 순수 UTF-8 String | JSON + `@class` 타입 정보 |
| **용량 (User 1건)** | 62 bytes | 108 bytes (**+74%**) |
| **`@class` 필드** | 없음 | 있음 (패키지 경로 박힘) |
| **입력 타입** | String만 | Any 객체 |
| **역직렬화** | String 반환 (수동 파싱) | 객체 자동 복원 (`@class` 기반) |
| **코드 패턴** | `ObjectMapper` 직접 호출 | 객체 그대로 set/get |
| **이종 언어 호환** | ✅ Node/Python 바로 읽음 | ⚠️ `@class` 노이즈 |
| **패키지 리팩터링** | 안전 (타입 정보 0) | **캐시 깨짐** (패키지 경로 바뀌면) |

## 실제 저장된 값 비교

### StringRedisSerializer (String 쪽)
```json
{"id":"1","name":"alice","email":"alice@example.com","age":30}
```
→ 어떤 언어든 JSON.parse 가능. 깨끗함.

### GenericJackson2JsonRedisSerializer (Jackson 쪽)
```json
{"@class":"com.example.jacksonser.domain.User","id":"1","name":"alice","email":"alice@example.com","age":30}
```
→ `@class`에 Java 패키지 경로 박힘. Node/Python 입장에선 쓸데없는 필드.

## Kotlin data class 사용 시 주의 (함정)

`GenericJackson2JsonRedisSerializer()` 기본 생성자 사용 시 **Kotlin data class 역직렬화 실패**:

```
Cannot construct instance of `User` (no Creators, like default constructor, exist)
```

**원인:** 기본 ObjectMapper엔 `jackson-module-kotlin` 등록 안 됨.

**해결 (본 POC `jackson-serializer/config/RedisConfig.kt` 참조):**
```kotlin
val kotlinAwareMapper = ObjectMapper().apply {
    registerModule(KotlinModule.Builder().build())
    activateDefaultTypingAsProperty(
        BasicPolymorphicTypeValidator.builder().allowIfBaseType(Any::class.java).build(),
        ObjectMapper.DefaultTyping.EVERYTHING,
        "@class",
    )
}
GenericJackson2JsonRedisSerializer(kotlinAwareMapper)
```

**추가 함정:** Kotlin data class는 `final`이라 `DefaultTyping.NON_FINAL`로는 `@class` 안 붙음 → `EVERYTHING`으로 강제.

## API 엔드포인트

양쪽 모두 동일 (키 prefix만 다름):

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/users` | User 저장 (`{id, name, email, age}`) |
| GET | `/api/users/{id}` | User 조회 (역직렬화 테스트) |
| DELETE | `/api/users/{id}` | 삭제 |
| GET | `/api/users/{id}/raw` | Spring이 읽은 원시 값 확인 |

## 정리

```bash
# 컨테이너 + 데이터 삭제
docker compose down -v

# 또는 데이터 유지
docker compose down
```

## 학습 포인트

1. **저장 형식 직접 확인** — `redis-cli GET` 한 번이 백 마디 말보다 명확
2. **용량 차이 체감** — 작은 객체도 75% 차이. 대규모 캐시에선 누적 비용
3. **Polyglot 호환성** — Node/Python이 같은 Redis 읽을 시 `@class`가 노이즈
4. **Kotlin 함정** — GenericJackson2Json 기본 생성자론 Kotlin data class 역직렬화 실패
5. **안전한 실무 패턴** — Polyglot 환경에선 StringRedisSerializer + 서비스단 수동 ObjectMapper 추천
