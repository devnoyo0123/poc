# Rate-Limited API POC

**POC 목적: 요청을 모아서 1초에 최대 2번 처리.**

외부 API 요청 수신 → rate limit(1초 2회) 적용.

## 기술 스택

- Java 17, Spring Boot 2.7.8
- Gradle (Kotlin DSL)
- Redis (분산 rate limit)
- RxJava 3 (Flowable backpressure)
- Servlet Stack

---

## RxJava 전략 흐름 (기본)

```
[요청 유입] ──→ Flowable ──onBackpressureBuffer(100)──→ [Subscriber]
     많음                         ↑
                                  버퍼 초과 시 drop/error
                                        │
                                        ▼
                              request(1) → token 획득 → 외부 API 호출
                                        │
                                        └── Redis Token Bucket (2/초)
```

- **Flowable**: 요청 flood → `onBackpressureBuffer`로 버퍼
- **Subscriber**: `request(1)` → Redis token 획득 → 외부 API 호출
- **Redis Token Bucket**: 전역 1초 2회 제한

---

## Backpressure 전략

| 전략 | 설명 | 활성화 |
|------|------|--------|
| **rxjava** (기본) | Flowable + onBackpressureBuffer + Redis Token Bucket | `app.backpressure.strategy=rxjava` |
| **blocking-queue** | Bounded BlockingQueue + Worker | `app.backpressure.strategy=blocking-queue` |

---

## 아키텍처

### 1. 분산 Rate Limit (토큰 버킷)

- Redis Lua 스크립트로 원자적 토큰 refill + acquire
- 스케일 아웃 시에도 **전역** 1초당 2회 보장

### 2. Backpressure (전략 패턴)

- **rxjava**: Semaphore + PublishProcessor + `onBackpressureBuffer`, Subscriber에서 token 획득 후 API 호출
- **blocking-queue**: `BlockingQueue` + Worker 스레드

큐/버퍼 가득 차면 HTTP 503 반환.

---

## 실행

```bash
# Redis 실행
docker-compose up -d

# 애플리케이션
./gradlew bootRun
```

## API

```bash
# 처리 요청 (동기, JSON)
curl -X POST http://localhost:8090/api/process \
  -H "Content-Type: application/json" \
  -d '{"payload":"test"}'
# → 처리 완료 후 200 + 외부 API 응답 (JSON)

# 헬스
curl http://localhost:8090/api/health
```

## 설정

| 설정 | 설명 | 기본값 |
|------|------|--------|
| `app.backpressure.strategy` | rxjava \| blocking-queue | rxjava |
| `app.rate-limit.permits-per-second` | 초당 허용 호출 수 | 2 |
| `app.backpressure.queue-capacity` | 버퍼/큐 크기 | 100 |

---

## Rate limit 검증

### 1. 로그 (RATE_LIMIT_VERIFY)

실제 외부 API 호출마다 로그 출력:

```
[RATE_LIMIT_VERIFY] external_api_call epoch_ms=1739012345678
```

```bash
# 로그에서 호출 수 확인 (1분 기준, 1초 2회 → 최대 120건)
grep "external_api_call" application.log | wc -l
```

### 2. GET /api/stats

```bash
# 최근 60초 초당 호출 수, maxCallsPerSecond ≤ 2 확인
curl http://localhost:8090/api/stats
# → {"rateLimitOk": true, "maxCallsPerSecond": 2, "perSecond": [...], ...}
```

### 3. k6 부하 테스트

```bash
# Redis + 앱 실행 후
k6 run k6-script.js

# 테스트 종료 후 검증
curl http://localhost:8090/api/stats?seconds=60
# rateLimitOk: true, maxCallsPerSecond ≤ 2 확인
```
