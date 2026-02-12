# SSE WebFlux vs Servlet Performance Projects

이 프로젝트 모음은 Spring WebFlux와 Servlet Stack 간의 Server-Sent Events(SSE) 성능을 비교하고, Reactive 프로그래밍 패턴을 탐구합니다.

## 📁 프로젝트 구조

```
sse-webflux-servlet-projects/
├── retry-server/              # 재시도 로직 테스트용 불안정 API 서버
├── sse-performance-comparison/ # WebFlux vs Servlet SSE 성능 비교
├── webflux-queue-service/     # WebFlux 기반 Reactive 대기열 서비스
└── webflux-user-api/          # WebFlux + R2DBC 사용자 API
```

---

## 🔄 1. retry-server

불안정한 외부 API를 시뮬레이션하는 서버입니다. 클라이언트의 재시도 로직을 테스트하기 위해 주기적 실패를 발생시킵니다.

### 🎯 주요 기능
- **주기적 실패 시뮬레이션**: 실패율을 동적으로 조절
- **다양한 엔드포인트**: 사용자, 주문 조회 API 제공
- **안정 엔드포인트**: 베이스라인 성능 측정용
- **Health Check**: 서버 상태 및 실패율 모니터링

### 🚀 실행 방법

```bash
cd retry-server
./gradlew bootRun
```

### 📡 API Endpoints

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/users/{id}` | 사용자 조회 (불안정) |
| GET | `/api/orders/{id}` | 주문 조회 (불안정) |
| GET | `/api/users/{id}/stable` | 사용자 조회 (안정) |
| GET | `/api/health` | 서버 헬스 체크 |
| POST | `/api/admin/failure-rate` | 실패율 설정 |
| GET | `/api/admin/failure-rate` | 현재 실패율 조회 |

### 🧪 테스트 예시

```bash
# 1. 헬스 체크
curl http://localhost:8080/api/health

# 2. 사용자 조회 (재시도 테스트용 - 실패할 수 있음)
curl http://localhost:8080/api/users/1

# 3. 안정 사용자 조회 (항상 성공)
curl http://localhost:8080/api/users/1/stable

# 4. 실패율 50%로 설정
curl -X POST http://localhost:8080/api/admin/failure-rate \
  -H "Content-Type: application/json" \
  -d '{"rate": 0.5}'

# 5. 현재 실패율 확인
curl http://localhost:8080/api/admin/failure-rate
```

### 📊 응답 포맷

```json
// 사용자 조회 응답
{
  "userId": 1,
  "userName": "User-1",
  "email": "user1@example.com"
}

// 주문 조회 응답
{
  "orderId": 1,
  "userId": 1,
  "totalAmount": 100.50,
  "status": "COMPLETED"
}

// 헬스 체크 응답
{
  "status": "UP",
  "service": "retry-server",
  "failureRate": "50.00%"
}
```

---

## ⚡️ 2. sse-performance-comparison

WebFlux와 Servlet Stack 간의 SSE 성능을 실시간 비교하는 프로젝트입니다. C10K(10,000 동시 연결) 부하 테스트로 각 스택의 리소스 사용 효율성을 증명합니다.

### 🎯 테스트 목표
- **실시간 가격 브로드캐스트 시나리오**: 두 스택의 동시 처리 능력 비교
- **인메모리 데이터 생성으로 순수 네트워크/스레드 모델 차이 집중 비교**
- **C10K 부하 테스트로 리소스 사용량(메모리, 스레드, CPU) 차이 증명**

### 🏗️ 아키텍처

```
sse-performance-comparison/
├── shared/                  # 공통 모듈
│   └── src/main/kotlin/com/example/sse/
│       ├── domain/StockPrice.kt
│       ├── service/PriceGenerator.kt
│       └── config/MetricsConfig.kt
├── servlet-stack/            # Servlet Stack (Tomcat, port 8081)
│   └── src/main/kotlin/com/example/sse/servlet/
│       ├── controller/PriceController.kt
│       └── config/TomcatConfig.kt
├── webflux-stack/            # WebFlux Stack (Netty, port 8082)
│   └── src/main/kotlin/com/example/sse/webflux/
│       ├── handler/PriceHandler.kt
│       ├── config/RouterConfig.kt
│       └── config/NettyConfig.kt
├── docker/                   # Prometheus + Grafana
│   ├── docker-compose.yml
│   ├── prometheus/prometheus.yml
│   └── grafana/dashboards/
├── load-test/k6/             # k6 부하 테스트
│   ├── sse-test.js
│   └── scenarios/
│       ├── c100.js
│       ├── c1k.js
│       ├── c5k.js
│       └── c10k.js
├── scripts/
│   ├── start-all.sh            # 모든 서비스 시작
│   ├── stop-all.sh             # 모든 서비스 중지
│   └── benchmark.sh            # 벤치마크 실행
└── results/                  # 테스트 결과 (Git 추적 제외)
```

### 🚀 실행 방법

#### 1. 모든 서비스 시작 (Docker)

```bash
cd sse-performance-comparison
./scripts/start-all.sh
```

#### 2. 수동 테스트

```bash
# Servlet Stack 테스트
curl -N http://localhost:8081/api/prices/stream

# WebFlux Stack 테스트
curl -N http://localhost:8082/api/prices/stream
```

#### 3. 벤치마크 실행

```bash
./scripts/benchmark.sh
```

### 📡 모니터링

| 대시보드 | URL | 로그인 |
|---------|-----|-------|
| **Grafana** | http://localhost:3000 | admin / admin |
| **Prometheus** | http://localhost:9090 | - |
| **Servlet Actuator** | http://localhost:8081/actuator | - |
| **WebFlux Actuator** | http://localhost:8082/actuator | - |

### 📊 예상 결과

| 메트릭 | Servlet (Tomcat) | WebFlux (Netty) |
|--------|------------------|-----------------|
| **스레드 수** | ~200 (maxThreads) | ~4 (event loop) |
| **메모리 사용** | 높음 (스레드 스택 오버헤드) | 낮음 |
| **CPU 사용** | 높음 (컨텍스트 스위칭) | 낮음 |
| **C10K 처리** | 제한 (스레드 풀) | 원할 |

### 🧪 개별 부하 테스트

```bash
# C100 테스트 (100 연결)
k6 run load-test/k6/scenarios/c100.js

# C1K 테스트 (1,000 연결)
k6 run load-test/k6/scenarios/c1k.js

# C5K 테스트 (5,000 연결)
k6 run load-test/k6/scenarios/c5k.js

# C10K 테스트 (10,000 연결)
k6 run load-test/k6/scenarios/c10k.js
```

### 🔑 핵심 메트릭

Grafana 대시보드에서 다음 메트릭을 확인하세요:

1. **JVM Thread Count** - 가장 중요한 메트릭!
   - `jvm_threads_live_threads{application="servlet-stack"}`
   - `jvm_threads_live_threads{application="webflux-stack"}`

2. **Active SSE Connections**
   - `sse_servlet_connections_active`
   - `sse_webflux_connections_active`

3. **JVM Heap Memory**
   - `jvm_memory_used_bytes{area="heap"}`

4. **CPU Usage**
   - `system_cpu_usage`

---

## 🚦 3. webflux-queue-service

Spring WebFlux를 활용한 리액티브 대기열(Queue) 서비스입니다. Redis Sorted Set과 R2DBC PostgreSQL을 사용하여 완전한 non-blocking 아키텍처를 구현했습니다.

### 🎯 주요 기능

#### Phase 1: Core WebFlux + Redis Queue
- ✅ Spring WebFlux 기반 REST API
- ✅ Redis Sorted Set을 활용한 FIFO 대기열
- ✅ Reactive 프로그래밍 (Mono/Flux)
- ✅ Functional Routing
- ✅ 글로벌 예외 처리
- ✅ Bean Validation

#### Phase 2: Database Integration
- ✅ R2DBC PostgreSQL 연동
- ✅ Flyway 마이그레이션
- ✅ 큐 히스토리 영구 저장
- ✅ Analytics API
- ✅ 평균 대기 시간 통계

### 🚀 실행 방법

#### 1. Prerequisites

- Java 21+
- Docker & Docker Compose
- Gradle 8.x

#### 2. Docker 컨테이너 시작

```bash
cd webflux-queue-service
docker-compose up -d
```

Redis와 PostgreSQL이 시작됩니다:
- Redis: `localhost:6379`
- PostgreSQL: `localhost:5432`

#### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

애플리케이션이 `http://localhost:8080`에서 실행됩니다.

#### 4. Health Check

```bash
curl http://localhost:8080/actuator/health
```

### 📡 API 문서

#### Queue API

##### 1. Join Queue (대기열 참가)

```bash
POST /api/v1/queue/join
Content-Type: application/json

{
  "userId": "user123",
  "priority": "NORMAL",
  "metadata": {
    "clientType": "mobile"
  }
}

# Response
{
  "queueId": "550e8400-e29b-41d4-a716-44665440000",
  "position": 15,
  "estimatedWaitTime": "PT5M",
  "joinedAt": "2026-02-08T10:30:00Z"
}
```

##### 2. Get Position (현재 위치 조회)

```bash
GET /api/v1/queue/position/{userId}

# Response
{
  "queueId": "550e8400-e29b-41d4-a716-44665440000",
  "currentPosition": 10,
  "estimatedWaitTime": "PT3M",
  "status": "WAITING"
}
```

##### 3. Leave Queue (대기열 나가기)

```bash
DELETE /api/v1/queue/{userId}

# Response: 204 No Content
```

##### 4. Queue Status (큐 상태 조회)

```bash
GET /api/v1/queue/status

# Response
{
  "totalInQueue": 25,
  "processingRate": 5.0,
  "averageWaitTime": "PT5M30S",
  "queueStatus": "ACTIVE"
}
```

##### 5. Process Queue (다음 배치 처리 - 관리자용)

```bash
POST /api/v1/queue/process

# Response
[
  {
    "queueId": "550e8400-e29b-41d4-a716-44665440000",
    "userId": "user123",
    "waitTime": "PT4M32S"
  }
]
```

#### Analytics API

##### 1. Average Wait Time (평균 대기 시간)

```bash
GET /api/v1/analytics/wait-time

# Response
{
  "averageWaitTime": "PT5M15S",
  "sampleSize": 150,
  "period": "P1D"
}
```

##### 2. Queue Statistics (큐 통계)

```bash
GET /api/v1/analytics/statistics

# Response
{
  "totalProcessed": 1500,
  "averageWaitTime": "PT5M15S",
  "currentQueueSize": 25
}
```

##### 3. User History (사용자 히스토리)

```bash
GET /api/v1/analytics/history/{userId}

# Response
[
  {
    "queueId": "550e8400-e29b-41d4-a716-44665440000",
    "status": "COMPLETED",
    "joinedAt": "2026-02-08T10:30:00Z",
    "processedAt": "2026-02-08T10:35:32Z",
    "waitTime": "PT5M32S"
  }
]
```

##### 4. Recent History (최근 히스토리)

```bash
GET /api/v1/analytics/recent?limit=10

# Response: (same as user history)
```

### 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 통합 테스트만 실행
./gradlew integrationTest

# 커버리지 리포트
./gradlew jacocoTestReport
```

### ⚙️ 설정

주요 설정은 `application.yml`에서 관리합니다:

```yaml
queue:
  max-size: 1000              # 최대 대기열 크기
  processing-rate: 10             # 분당 처리 속도
  active-ttl: 300                 # Active 토큰 TTL (초)
  scheduler:
    process-interval: 5000        # 처리 주기 (밀리초)
    batch-size: 10                 # 한 번에 처리할 인원
```

---

## 👤 4. webflux-user-api

Spring WebFlux 기반의 Reactive REST API 예제 프로젝트입니다. R2DBC를 사용하여 비동기/논블로킹 데이터베이스 액세스를 구현했습니다.

### 🛠 기술 스택

- **Spring Boot**: 3.2.0
- **Spring WebFlux**: Reactive Web 프레임워크
- **Spring Data R2DBC**: Reactive Database Access
- **Kotlin**: 1.9.20
- **H2**: 인메모리 데이터베이스 (R2DBC)

### 📡 API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/users` | 전체 사용자 조회 |
| GET | `/api/users/{id}` | ID로 사용자 조회 |
| GET | `/api/users/search?username={username}` | 사용자명으로 검색 |
| POST | `/api/users` | 사용자 생성 |
| PUT | `/api/users/{id}` | 사용자 수정 |
| DELETE | `/api/users/{id}` | 사용자 삭제 |
| DELETE | `/api/users` | 전체 사용자 삭제 |

### 🚀 실행 방법

```bash
cd webflux-user-api
./gradlew bootRun
```

### 🧪 예제 요청

#### 사용자 생성

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "email": "john@example.com"}'
```

#### 전체 사용자 조회

```bash
curl http://localhost:8080/api/users
```

#### 사용자 검색

```bash
curl "http://localhost:8080/api/users/search?username=john"
```

#### 사용자 수정

```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"username": "johnny", "email": "johnny@example.com"}'
```

#### 사용자 삭제

```bash
curl -X DELETE http://localhost:8080/api/users/1
```

---

## 📦 공통 요구사항

모든 프로젝트에 필요한 공통 요구사항입니다:

### 필수 조건

- **Java**: 21+ (LTS)
- **Gradle**: 8.x
- **Docker**: 서비스 실행용 (선택 프로젝트)

### 선택 조건

- **k6**: 부하 테스트 (sse-performance-comparison)
- **PostgreSQL**: 데이터베이스 (webflux-queue-service)
- **Redis**: 캐시/대기열 (webflux-queue-service)

## 🔧 개발 환경 설정

```bash
# Git 설정
git config --global user.name "your-name"
git config --global user.email "your-email@example.com"

# SSH 설정 (여러 계정 사용 시 ~/.ssh/config 참고)
Host github.com-personal
  HostName github.com
  User git
  IdentityFile ~/.ssh/personal_key
  IdentitiesOnly yes
```

## 📝 라이선스

MIT License

## 👥 기여

이 프로젝트들은 학습과 실험을 위해 작성되었습니다. 각 프로젝트는 특정 기술 스택이나 패턴을 탐구하는 것을 목적으로 합니다.

---

## 📚 추가 학습 자료

- [Spring WebFlux 공식 문서](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [R2DBC 공식 문서](https://r2dbc.io/)
- [SSE (Server-Sent Events) MDN](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [k6 부하 테스트 도구](https://k6.io/)
- [Grafana 모니터링](https://grafana.com/)
- [Prometheus 모니터링](https://prometheus.io/)
