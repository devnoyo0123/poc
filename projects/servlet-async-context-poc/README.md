# Servlet 3.0 AsyncContext 성능 비교 POC

Java 21 + Spring Boot 3.3.5 환경에서 동일한 외부 API 3종 병렬 호출 시, **Blocking 컨트롤러 패턴**(`allOf().join()`)과 **Async 컨트롤러 패턴**(`CompletableFuture` 리턴)의 처리량 차이를 비교합니다. Tomcat 스레드 풀을 `max=10`으로 고정하여 Blocking 시 스레드 고갈 효과를 극대화했습니다.

## 개요

세 개의 외부 서비스(A·B·C, 각 1초 지연)를 동시에 호출해 하나의 응답으로 취합하는 상황입니다. 두 컨트롤러는 **서비스 계층(CompletableFuture + 전용 Executor)이 완전히 동일**하며, 오직 컨트롤러 반환 방식만 다릅니다.

| 구분 | BlockingController | AsyncController |
| --- | --- | --- |
| 반환 타입 | `AggregatedResponse` | `CompletableFuture<AggregatedResponse>` |
| Tomcat 스레드 | `allOf(...).join()` 호출로 **1초간 블로킹** | 요청을 `AsyncContext`로 전환 후 **즉시 반납** |
| 서비스 호출 | `CompletableFuture.supplyAsync(..., externalCallExecutor)` (동일) | (동일) |

## 기술 스택

- **Java 21** (`sourceCompatibility = '21'`, Virtual Thread 비활성성 — Platform Thread로 차이 관찰)
- **Spring Boot 3.3.5** (`spring-boot-starter-web` only — WebFlux/JPA/DB 없음)
- **Tomcat** (`threads.max=10`, `accept-count=100`)
- **Lombok** (`@Data` DTO)
- **Gradle (Groovy DSL)**

## 핵심 원리

### Blocking 패턴 — Tomcat 스레드 점유
```
요청1..10  ──▶ Tomcat 스레드1..10 (each blocks ~1000ms on .join())
요청11..50 ──▶ accept-count 큐 대기
```
- 풀(10)이 1초 동안 가득 참 → 50개 동시 요청은 5배치(5 × 1s)로 직렬화
- 예상 wall time ≈ `ceil(50 / 10) × 1s = 5s`

### Async 패턴 — Tomcat 스레드 즉시 반납
```
요청1..50 ──▶ Tomcat 스레드 (1ms 내 AsyncContext 전환 후 반납)
                  └─ 서비스 CompletableFuture는 externalCallExecutor에서 실행
```
- Tomcat 스레드는 거의 1ms 만에 반납 → 풀(10)로 50개 요청 즉시 수용
- 외부 호출(1s)은 별도 Executor(cached pool, 사실상 무제한)에서 병렬 실행
- 예상 wall time ≈ `1s`

## 빠른 시작

```bash
cd projects/servlet-async-context-poc

# 1. 앱 시작 (빌드 포함, 백그라운드 실행)
./load-test/run.sh start

# 2. 단일 요청 점검
./load-test/run.sh single

# 3. 50 동시 요청 비교 (핵심!)
./load-test/run.sh compare 50

# 4. 100 동시 요청 비교 (더 극단)
./load-test/run.sh compare 100

# 5. 응답 시간 분포
./load-test/run.sh distribution 50

# 6. 전체 시나리오
./load-test/run.sh all

# 앱 중지
./load-test/run.sh stop
```

포트 변경 시: `PORT=9000 ./load-test/run.sh start`

## 테스트 러너 명령어

| 명령 | 설명 |
| --- | --- |
| `./run.sh start` | 앱 백그라운드 시작 (자동 빌드) |
| `./run.sh stop` | 앱 중지 |
| `./run.sh status` | 앱 상태 확인 |
| `./run.sh single` | 단일 요청 — 정상 점검 (둘 다 ~1초) |
| `./run.sh blocking [N]` | `/blocking` N 동시 (기본 50) |
| `./run.sh async [N]` | `/async` N 동시 (기본 50) |
| `./run.sh compare [N]` | side-by-side 비교 (기본 50) |
| `./run.sh distribution [N]` | 응답 시간 분포 히스토그램 (기본 100) |
| `./run.sh all` | 전체 시나리오 순차 실행 |

## API 엔드포인트

| Method | Path | 반환 | 특징 |
| --- | --- | --- | --- |
| GET | `/blocking` | `AggregatedResponse` | Tomcat thread blocks ~1s |
| GET | `/async` | `CompletableFuture<AggregatedResponse>` | Tomcat thread released via AsyncContext |

### 응답 예시
```json
{
  "a": { "userInfo": "user-info from A @ 1730000000000" },
  "b": { "orderInfo": "order-info from B @ 1730000000000" },
  "c": { "paymentInfo": "payment-info from C @ 1730000000000" },
  "elapsedMs": 1008,
  "mode": "ASYNC"
}
```

## 실측 결과

환경: macOS, Java 21, Tomcat threads.max=10, external-api.delay-ms=1000

### 동시 50 요청

| 지표 | BLOCKING | ASYNC | 개선 |
| --- | --- | --- | --- |
| Wall 시간 | 4.97s | 1.01s | **4.9x** |
| 최소 지연 | 1.00s | 1.00s | — |
| 평균 지연 | 2.98s | 1.01s | **3.0x** |
| 최대 지연 | 4.97s | 1.01s | **4.9x** |
| Throughput | 10.1 rps | 49.4 rps | **4.9x** |

### 동시 100 요청

| 지표 | BLOCKING | ASYNC | 개선 |
| --- | --- | --- | --- |
| Wall 시간 | 9.94s | 1.01s | **9.9x** |
| 평균 지연 | 5.47s | 1.01s | **5.4x** |
| 최대 지연 | 9.94s | 1.01s | **9.9x** |
| Throughput | 10.1 rps | 99.2 rps | **9.8x** |

### 응답 시간 분포 (동시 50)

**BLOCKING** — 계단식 직렬화 (Tomcat 풀 10개가 5배치 처리):
```
1.0s × 10    ← 첫 배치
2.0s × 10    ← 두 번째 배치 (1초 대기 후 실행)
3.0s × 10
4.0s × 10
5.0s × 10    ← 마지막 배치 (4초 대기)
```

**ASYNC** — 전부 동시 처리:
```
1.0s × 50    ← 50개가 한 번에 완료
```

## 핵심 코드 설명

### BlockingController — Tomcat 스레드 블로킹
```java
@GetMapping
public AggregatedResponse blocking() {
    long start = System.currentTimeMillis();

    CompletableFuture<ResponseA> fa = serviceA.callAsync();
    CompletableFuture<ResponseB> fb = serviceB.callAsync();
    CompletableFuture<ResponseC> fc = serviceC.callAsync();

    CompletableFuture.allOf(fa, fb, fc).join();   // ★ Tomcat thread blocks here ~1s

    long elapsed = System.currentTimeMillis() - start;
    return new AggregatedResponse(fa.join(), fb.join(), fc.join(), elapsed, "BLOCKING");
}
```

### AsyncController — `CompletableFuture` 반환
```java
@GetMapping
public CompletableFuture<AggregatedResponse> async() {   // ★ 반환 타입이 CF
    long start = System.currentTimeMillis();

    CompletableFuture<ResponseA> fa = serviceA.callAsync();
    CompletableFuture<ResponseB> fb = serviceB.callAsync();
    CompletableFuture<ResponseC> fc = serviceC.callAsync();

    return CompletableFuture.allOf(fa, fb, fc)
            .thenApply(v -> {
                long elapsed = System.currentTimeMillis() - start;
                return new AggregatedResponse(fa.join(), fb.join(), fc.join(), elapsed, "ASYNC");
            });
}
```

### 왜 Async가 빠른가? — Servlet 3.0 AsyncContext

Spring MVC에서 컨트롤러가 `CompletableFuture`를 반환하면 `DispatcherServlet`은 요청을 `AsyncContext`로 전환한 뒤 **Tomcat HTTP 스레드를 즉시 풀로 반납**합니다. 이후 CF가 완료되면 AsyncContext 복귀용(보통 다른) 스레드로 응답을 직렬화해 보냅니다. 즉 외부 I/O 대기 시간 동안 Tomcat의 worker 스레드를 소비하지 않으므로, 스레드 풀이 작더라도 수백~수천의 동시 요청을 감당할 수 있습니다. Blocking 패턴은 I/O 대기를 worker 스레드 위에서 직접 `sleep`하는 것과 같아, 풀 크기가 동시성의 경직된 상한이 됩니다.

### 스레드 흐름도

```
[Blocking 패턴]
 Tomcat Thread #1 ─[1초 대기]─▶ 응답
 Tomcat Thread #2 ─[1초 대기]─▶ 응답
 ...
 Tomcat Thread #10 ─[1초 대기]─▶ 응답
 (11번째 요청부터는 큐에서 1초 대기 후 스레드 할당)

[Async 패턴]
 Tomcat Thread #1 ─[1ms]─▶ 반납  ──────── (CF 완료 대기, 스레드 점유 없음)
 Tomcat Thread #2 ─[1ms]─▶ 반납  ──────── ...
 ...
 Tomcat Thread #10 ─[1ms]─▶ 반납 ──────── ...
                                              ↓ CF 완료 시점
 ext-call-N 스레드 ─[응답 직렬화]─▶ Tomcat Thread 재빌려 응답 write
```

## 결과 해석 가이드

### Tomcat `threads.max=10`으로 둔 이유
실서비스 Tomcat 기본값(`200`)에서는 Blocking 패턴도 소규모 부하(50 RPS)엔 여유가 있어 차이가 안 보입니다. 풀을 `10`으로 좁혀 **스레드가 고갈되는 임계점**을 의도적으로 낮추면 두 패턴의 처리량 격차가 선명히 드러납니다.

### 확인할 지표
- **Throughput (RPS)**: `count / wall_time`. Async 쪽이 pool_size 배 이상 높게 나와야 정상.
- **Latency 분포**: `./run.sh distribution N` 출력. Blocking은 1s/2s/3s/...s 계단식 분포, Async는 대부분 ~1s에 몰림.
- **HTTP 상태 코드**: `accept-count=100` 초과 시 503 — 이때는 부하가 극단적인 것.

### 스레드 풀 모니터링 방법
- **Actuator**: `spring-boot-starter-actuator` 추가 후 `GET /actuator/metrics/tomcat.threads.busy` · `tomcat.threads.current` · `tomcat.threads.config.max`
- **JConsole / VisualVM**: `org.apache.tomcat.util.threads.ThreadPool` MBean의 `activeCount` 관찰
- **jstack**: `jstack <PID> | grep -E "http-nio-|ext-call-"` — Blocking 중이면 `http-nio-exec-*`가 RUNNABLE, Async 중이면 `ext-call-*`가 활동

## 설정

`application.yml`에서 지연 시간 조절 가능:

```yaml
external-api:
  delay-ms: 1000   # 각 서비스당 시뮬레이션 지연 (ms)
```

Tomcat 풀 크기 변경으로 임계점 튜닝:

```yaml
server:
  tomcat:
    threads:
      max: 10       # 이 값을 올리면 Blocking도 버틸 수 있음 — 차이 축소
```

## 주의사항

- 본 POC는 외부 API를 `Thread.sleep`으로 흉내 낸 시뮬레이션입니다. 실환경에서는 네트워크 왕복·타임아웃·재시도가 추가됩니다.
- `externalCallExecutor`는 cached thread pool(사실상 무제한)입니다. 서비스 계층이 병목이 되지 않도록 해 **Tomcat 스레드 풀만 유일한 변수**로 만들기 위함입니다. 실서비스에서는 커넥션 풀 사이즈/외부 서비스 TPS 한계가 별도 병목이 됩니다.
- Async 패턴도 외부 서비스 자체가 포화되면 근본 한계는 같습니다 — Circuit Breaker/Rate Limit은 별도 검토 필요.
- 단일 요청(동시성 1)에서는 두 패턴의 차이가 없습니다. **차이는 동시성이 풀 크기를 초과할 때 폭발**합니다.
