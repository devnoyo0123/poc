# WebFlux Queue Service POC

Spring WebFlux를 활용한 리액티브 대기열(Queue) 서비스입니다. Redis Sorted Set과 R2DBC PostgreSQL을 사용하여 완전한 non-blocking 아키텍처를 구현했습니다.

## 기술 스택

- **Framework**: Spring Boot 3.2.2 with WebFlux
- **Language**: Kotlin 1.9.22
- **Queue Backend**: Redis (Sorted Set)
- **Database**: PostgreSQL with R2DBC
- **Build Tool**: Gradle Kotlin DSL
- **Java**: 21 (LTS)

## 주요 기능

### Phase 1: Core WebFlux + Redis Queue
- ✅ Spring WebFlux 기반 REST API
- ✅ Redis Sorted Set을 활용한 FIFO 대기열
- ✅ Reactive 프로그래밍 (Mono/Flux)
- ✅ Functional Routing
- ✅ 글로벌 예외 처리
- ✅ Bean Validation

### Phase 2: Database Integration
- ✅ R2DBC PostgreSQL 연동
- ✅ Flyway 마이그레이션
- ✅ 큐 히스토리 영구 저장
- ✅ Analytics API
- ✅ 평균 대기 시간 통계

## 프로젝트 구조

```
projects/webflux-queue-service/
├── src/main/kotlin/com/example/queue/
│   ├── QueueServiceApplication.kt      # Main application
│   ├── config/
│   │   ├── QueueProperties.kt          # Queue configuration properties
│   │   ├── RedisConfig.kt              # Redis configuration
│   │   ├── R2dbcConfig.kt              # R2DBC configuration
│   │   └── WebFluxConfig.kt            # WebFlux configuration
│   ├── domain/
│   │   ├── QueueItem.kt                # Queue item model
│   │   ├── QueueStatus.kt              # Status enum
│   │   ├── Priority.kt                 # Priority enum
│   │   ├── QueueEvent.kt               # Event model
│   │   └── QueueHistory.kt             # History entity (R2DBC)
│   ├── queue/
│   │   ├── QueueBackend.kt             # Queue interface
│   │   └── RedisQueueBackend.kt        # Redis implementation
│   ├── service/
│   │   ├── QueueService.kt             # Business logic
│   │   └── AnalyticsService.kt         # Analytics logic
│   ├── repository/
│   │   └── QueueHistoryRepository.kt   # R2DBC repository
│   ├── handler/
│   │   ├── QueueHandler.kt             # Queue HTTP handler
│   │   └── AnalyticsHandler.kt         # Analytics HTTP handler
│   ├── router/
│   │   ├── QueueRouter.kt              # Queue routes
│   │   └── AnalyticsRouter.kt          # Analytics routes
│   ├── dto/
│   │   ├── JoinQueueRequest.kt
│   │   ├── JoinQueueResponse.kt
│   │   ├── PositionResponse.kt
│   │   ├── QueueStatusResponse.kt
│   │   ├── ProcessResponse.kt
│   │   └── AnalyticsResponse.kt
│   └── exception/
│       ├── QueueException.kt
│       └── GlobalExceptionHandler.kt
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   └── db/migration/
│       └── V1__init_schema.sql
└── docker-compose.yml
```

## 시작하기

### 1. Prerequisites

- Java 21+
- Docker & Docker Compose
- Gradle 8.x

### 2. Docker 컨테이너 시작

```bash
docker-compose up -d
```

Redis와 PostgreSQL이 시작됩니다:
- Redis: `localhost:6379`
- PostgreSQL: `localhost:5432`

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

애플리케이션이 `http://localhost:8080`에서 실행됩니다.

### 4. Health Check

```bash
curl http://localhost:8080/actuator/health
```

## API 문서

### Queue API

#### 1. Join Queue (대기열 참가)

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
  "queueId": "550e8400-e29b-41d4-a716-446655440000",
  "position": 15,
  "estimatedWaitTime": "PT5M",
  "joinedAt": "2026-02-08T10:30:00Z"
}
```

#### 2. Get Position (현재 위치 조회)

```bash
GET /api/v1/queue/position/{userId}

# Response
{
  "queueId": "550e8400-e29b-41d4-a716-446655440000",
  "currentPosition": 10,
  "estimatedWaitTime": "PT3M",
  "status": "WAITING"
}
```

#### 3. Leave Queue (대기열 나가기)

```bash
DELETE /api/v1/queue/{userId}

# Response: 204 No Content
```

#### 4. Queue Status (큐 상태 조회)

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

#### 5. Process Queue (다음 배치 처리 - 관리자용)

```bash
POST /api/v1/queue/process

# Response
[
  {
    "queueId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "user123",
    "waitTime": "PT4M32S"
  }
]
```

### Analytics API

#### 1. Average Wait Time (평균 대기 시간)

```bash
GET /api/v1/analytics/wait-time

# Response
{
  "averageWaitTime": "PT5M15S",
  "sampleSize": 150,
  "period": "P1D"
}
```

#### 2. Queue Statistics (큐 통계)

```bash
GET /api/v1/analytics/statistics

# Response
{
  "totalProcessed": 1500,
  "averageWaitTime": "PT5M15S",
  "currentQueueSize": 25
}
```

#### 3. User History (사용자 히스토리)

```bash
GET /api/v1/analytics/history/{userId}

# Response
[
  {
    "queueId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "COMPLETED",
    "joinedAt": "2026-02-08T10:30:00Z",
    "processedAt": "2026-02-08T10:35:32Z",
    "waitTime": "PT5M32S"
  }
]
```

#### 4. Recent History (최근 히스토리)

```bash
GET /api/v1/analytics/recent?limit=10

# Response: (same as user history)
```

## Redis 구조

### Sorted Set (대기열)

```
Key: queue:waiting
Type: ZSET
Score: timestamp (밀리초)
Member: userId
```

**핵심 연산**:
- `ZADD queue:waiting NX {timestamp} {userId}` - 대기열 추가 (중복 방지)
- `ZRANK queue:waiting {userId}` - 현재 순서 조회 (O(log N))
- `ZPOPMIN queue:waiting {count}` - FIFO 방식으로 다음 N명 처리
- `ZCARD queue:waiting` - 전체 대기자 수

### Active Tokens

```
Key: active:{userId}
Type: STRING
Value: token
TTL: 300 seconds (5분)
```

## Database Schema

```sql
CREATE TABLE queue_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    queue_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    wait_time_seconds INTEGER,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 통합 테스트만 실행
./gradlew integrationTest

# 커버리지 리포트
./gradlew jacocoTestReport
```

## 설정

주요 설정은 `application.yml`에서 관리합니다:

```yaml
queue:
  max-size: 1000              # 최대 대기열 크기
  processing-rate: 10         # 분당 처리 속도
  active-ttl: 300             # Active 토큰 TTL (초)
  scheduler:
    process-interval: 5000    # 처리 주기 (밀리초)
    batch-size: 10            # 한 번에 처리할 인원
```

## 학습 포인트

### Reactive Programming
- `Mono<T>`: 0 또는 1개의 값을 emit하는 Publisher
- `Flux<T>`: 0~N개의 값을 emit하는 Publisher
- Operator chaining: `flatMap`, `map`, `filter`, `collectList` 등

### Redis Sorted Set
- O(log N) 성능으로 순서 조회
- FIFO 방식 구현 (timestamp as score)
- Atomic 연산 (동시성 안전)

### R2DBC
- Non-blocking 데이터베이스 접근
- Reactive Repository 패턴
- Flyway 마이그레이션

### WebFlux Patterns
- Functional Routing (Router + Handler)
- Global Exception Handling
- Bean Validation

## 다음 단계

- [ ] WebSocket/SSE for real-time position updates
- [ ] Priority queue implementation
- [ ] Rate limiting with Resilience4j
- [ ] Metrics with Micrometer
- [ ] Performance testing with Gatling

## 라이센스

MIT License
