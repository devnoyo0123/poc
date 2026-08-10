# tomcat-hikari-timeout-poc

Tomcat worker thread + HikariCP connection pool exhaustion / timeout POC.
느린 쿼리(`pg_sleep`)로 DB blocking 상황 재현. 200 동시 요청 시나리오를 초소형(5 concurrent)으로 축소 관찰.

## Config (초소형)

| Resource | Setting |
|---|---|
| Tomcat `server.tomcat.threads.max` | `5` |
| HikariCP `maximum-pool-size` | `2` |
| HikariCP `connection-timeout` | `2000` ms |
| Tomcat `server.connection-timeout` | `30000` ms |
| Slow query | `SELECT pg_sleep(5)` |
| Concurrent requests in test | `5` |

Scale-up mapping (원 질문):
- Tomcat max 5 → 200, HikariCP 2 → 20, 요청 5 → 200 동일 비율.

## Run

```bash
./gradlew test --rerun-tasks -i
```

Testcontainers Postgres 자동 기동.

## 관측된 타임라인 (실제 실행 로그 기반)

```
T=0ms      5개 POST 동시 도착 → 5개 Tomcat worker(http-nio-exec-1..5) ENTER controller
T=+1ms     전원 SlowService.slowQuery() 진입 → HikariCP.getConnection() 시도
T=+130ms   HikariCP pool stats = total=2, active=2, idle=0, waiting=3
              → exec-3, exec-4 가 connection 획득 후 pg_sleep(5) BLOCKED (socket read)
              → exec-1, exec-2, exec-5 는 Hikari internal handoff queue에서 WAITING(parked)
T=+2004ms  waiting 3개 thread에게 connection-timeout 도달
              → SQLTransientConnectionException("Connection is not available,
                                              request timed out after 2004ms.")
              → Spring 이 CannotGetJdbcConnectionException 으로 래핑
              → @RestControllerAdvice(DataAccessException handler) 가 503 응답
T=+5100ms  pg_sleep(5) 완료 → exec-3, exec-4 가 connection 반납 → 200 OK 응답
T=+5100ms  pool stats = total=2, active=0, idle=2, waiting=0
```

## 사용자 응답 (5개 요청 결과)

| Request idx | Status | Latency | Body |
|---|---|---|---|
| 0 | 503 | ~2s | `CannotGetJdbcConnectionException` ← `SQLTransientConnectionException` ← `Connection is not available, request timed out after 2005ms.` |
| 1 | 503 | ~2s | 동일 |
| 2 | 503 | ~2s | 동일 |
| 3 | 200 | ~5s | `{status: ok, workerThread: http-nio-...-exec-3, sleptSeconds: 5}` |
| 4 | 200 | ~5s | `{status: ok, workerThread: http-nio-...-exec-4, sleptSeconds: 5}` |

## Worker Thread 상태 분석

| Phase | Thread State | 비고 |
|---|---|---|
| `pg_sleep` 실행 중 | `RUNNABLE` (socket read) | JDBC 드라이버가 Postgres 응답 대기. OS 관점에선 socket epoll_wait |
| HikariCP 대기 중 | `WAITING (parked)` | `SynchronousQueue.transfer()` 에서 LockSupport.park |
| Controller 진입 전 | Tomcat worker pool 대기 | `threads.max` 초과 시 새 요청은 worker 가용까지 `connection-timeout` 대기 |

## Timeout 계층

1. **HikariCP `connection-timeout`** (2000ms)
   - Pool 가용 connection 부재 시 대기 최대 시간
   - 초과 → `SQLTransientConnectionException`
   - 본 POC에서 발생
2. **Tomcat `server.connection-timeout`** (30000ms)
   - HTTP 클라이언트가 요청 전송 완료하기까지 대기 시간
   - Tomcat worker 부재 시엔 해당 안 됨 (worker 점유는 별개)
3. **Tomcat worker 부재 시**: Tomcat 자체엔 worker 가용 타임아웃 명시적 설정 없음
   - `threads.max` 도달하면 새 요청은 accept queue 에서 worker freed 대기
   - 사실상 무한 대기(또는 socket keep-alive timeout)

## 핵심 학습 포인트

1. **200 concurrent vs HikariCP 20** → 180개는 connection 대기 → `connection-timeout` 폭발 → 180개 요청이 503
2. **200 Tomcat threads vs 200 concurrent** → 모든 worker 점유. 이 상태에선 201번째 요청 accept queue 적체
3. **SQLException → DataAccessException 변환**: `JdbcTemplate` 이 Hikari timeout 을 Spring `DataAccessException` 계층으로 래핑. `@ExceptionHandler(SQLException.class)` 만으로는 잡히지 않음 — `DataAccessException` handler 병행 필요
4. **Thread sleeping vs DB blocking**: 본 POC는 진짜 DB blocking(pg_sleep). `Thread.sleep` 시뮬레이션과 다른 점 = thread state(RUNNABLE on socket vs TIMED_WAITING)

## Architecture

```
ConcurrentSlowRequestTest (Kotest + Testcontainers)
        │  5 concurrent POSTs via ExecutorService + CompletableFuture
        ▼
SlowController (Tomcat http-nio-exec-* threads)
        │  POST /api/v1/slow?sleepSeconds=5
        ▼
SlowService (JdbcTemplate)
        │  jdbcTemplate.query("SELECT pg_sleep(?)", ...)
        ▼
HikariCP HikariPocPool (max=2)
        │  2개 즉시 할당, 3개 대기 → 2초 후 timeout
        ▼
Postgres 16 (Testcontainers)
```

## Files

```
src/main/java/com/example/tomcathikaritimeout/
├── TomcatHikariTimeoutPocApplication.java   # @SpringBootApplication
├── SlowController.java                       # POST /api/v1/slow
├── SlowService.java                          # jdbcTemplate + pg_sleep
└── SlowExceptionHandler.java                 # SQLException + DataAccessException → 503

src/main/resources/application.yml            # Tomcat/HikariCP config

src/test/kotlin/com/example/tomcathikaritimeout/
└── ConcurrentSlowRequestTest.kt              # Kotest + 5 concurrent requests

docker-compose.yml                            # postgres:16-alpine
```
