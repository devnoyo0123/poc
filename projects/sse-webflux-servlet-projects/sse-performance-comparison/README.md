# WebFlux vs Servlet Stack SSE 성능 비교 POC

이 POC는 WebFlux와 Servlet Stack 간의 SSE(Server-Sent Events) 성능을 비교합니다. 특히 C10K(10,000 동시 연결) 시나리오에서 WebFlux가 Servlet Stack보다 리소스를 훨씬 적게 사용한다는 가설을 검증합니다.

## 목표

- 실시간 가격 브로드캐스트 시나리오로 두 스택 비교
- 인메모리 데이터 생성으로 순수 네트워크/스레드 모델 차이 집중 비교
- C10K 부하 테스트로 리소스 사용량(메모리, 스레드, CPU) 차이 증명

## 프로젝트 구조

```
sse-performance-comparison/
├── shared/                     # 공통 모듈
│   └── src/main/kotlin/com/example/sse/
│       ├── domain/StockPrice.kt
│       ├── service/PriceGenerator.kt
│       └── config/MetricsConfig.kt
├── servlet-stack/              # Servlet Stack (Tomcat, port 8081)
│   └── src/main/kotlin/com/example/sse/servlet/
│       ├── controller/PriceController.kt
│       └── config/TomcatConfig.kt
├── webflux-stack/              # WebFlux Stack (Netty, port 8082)
│   └── src/main/kotlin/com/example/sse/webflux/
│       ├── handler/PriceHandler.kt
│       ├── config/RouterConfig.kt
│       └── config/NettyConfig.kt
├── docker/                     # Prometheus + Grafana
│   ├── docker-compose.yml
│   ├── prometheus/prometheus.yml
│   └── grafana/dashboards/
├── load-test/k6/               # k6 부하 테스트
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
└── results/                    # 테스트 결과
```

## 요구사항

- Java 21+
- Gradle 8+
- Docker & Docker Compose
- k6 (부하 테스트 도구)

## 빠른 시작

### 1. 모든 서비스 시작

```bash
cd /Users/colosseum_nohys/Documents/my/poc/projects/sse-performance-comparison
./scripts/start-all.sh
```

### 2. 수동 테스트

```bash
# Servlet Stack 테스트
curl -N http://localhost:8081/api/prices/stream

# WebFlux Stack 테스트
curl -N http://localhost:8082/api/prices/stream
```

### 3. 벤치마크 실행

```bash
./scripts/benchmark.sh
```

### 4. 모니터링

- **Grafana 대시보드:** http://localhost:3000 (admin/admin)
- **Prometheus:** http://localhost:9090
- **Servlet Actuator:** http://localhost:8081/actuator
- **WebFlux Actuator:** http://localhost:8082/actuator

## 서비스 URLs

| 서비스 | 포트 | Endpoints |
|--------|------|-----------|
| Servlet Stack | 8081 | `/actuator`, `/api/prices/stream` |
| WebFlux Stack | 8082 | `/actuator`, `/api/prices/stream` |
| Prometheus | 9090 | `/`, `/graph` |
| Grafana | 3000 | `/d/sse-comparison` |

## 예상 결과

| 메트릭 | Servlet (Tomcat) | WebFlux (Netty) |
|--------|------------------|-----------------|
| **스레드 수** | ~200 (maxThreads) | ~4 (event loop) |
| **메모리 사용** | 높음 (스레드 스택 오버헤드) | 낮음 |
| **CPU 사용** | 높음 (컨텍스트 스위칭) | 낮음 |
| **C10K 처리** | 제한 (스레드 풀) | 원활 |

## 개별 테스트 실행

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

## 모든 서비스 중지

```bash
./scripts/stop-all.sh
```

## 핵심 메트릭

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

## 기술 스택

- **Shared:** Spring Boot 3.2.3, Kotlin 1.9.22
- **Servlet:** Spring Web, Tomcat, SseEmitter
- **WebFlux:** Spring WebFlux, Netty, Reactive SSE
- **Monitoring:** Micrometer, Prometheus, Grafana
- **Testing:** k6
