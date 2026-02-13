# Project List

This document lists all POC projects in this repository.

## Template

To add a new project, copy and fill in this template:

```markdown
### [Project Name]

- **Path**: `projects/your-project-name`
- **Build Tool**: Gradle / Maven
- **Language**: Kotlin / Java
- **Spring Boot Version**: X.X.X
- **Description**: Brief description of what this project demonstrates
- **Status**: Planning / In Progress / Completed
- **Last Updated**: YYYY-MM-DD

#### Key Features
- Feature 1
- Feature 2

#### How to Run
\```bash
cd projects/your-project-name
./gradlew bootRun
# or
mvn spring-boot:run
\```
```

## Projects

### WebFlux User API

- **Path**: `projects/webflux-user-api`
- **Build Tool**: Gradle (Kotlin DSL)
- **Language**: Kotlin
- **Spring Boot Version**: 3.2.0
- **Description**: Spring WebFlux + R2DBC를 사용한 Reactive User CRUD API
- **Status**: Completed
- **Last Updated**: 2026-02-09

#### Key Features
- Spring WebFlux (Reactor) 비동기/논블로킹 REST API
- Spring Data R2DBC로 Reactive DB 액세스
- H2 인메모리 데이터베이스
- 전체 CRUD 작업 (생성, 조회, 수정, 삭제)
- 사용명 검색 기능
- 예외 처리 (UserNotFoundException, DuplicateUserException)

#### How to Run
```bash
cd projects/webflux-user-api
../gradlew bootRun
```

### SSE Performance Comparison

- **Path**: `projects/sse-performance-comparison`
- **Build Tool**: Gradle (Kotlin DSL)
- **Language**: Kotlin
- **Spring Boot Version**: 3.2.3
- **Description**: WebFlux vs Servlet Stack SSE 성능 비교 POC (C10K 테스트)
- **Status**: Completed
- **Last Updated**: 2026-02-11

#### Key Features
- Servlet Stack (Tomcat) + SseEmitter 구현 (port 8081)
- WebFlux Stack (Netty) + Reactive SSE 구현 (port 8082)
- 인메모리 가격 생성 서비스 (15개 심볼, 1초 간격)
- Prometheus + Grafana 모니터링 대시보드
- k6 부하 테스트 (C100, C1K, C5K, C10K 시나리오)
- JVM Thread Count, Memory, CPU 메트릭 비교

#### How to Run
```bash
cd projects/sse-performance-comparison

# Start all services (Prometheus, Grafana, Servlet, WebFlux)
./scripts/start-all.sh

# Run benchmark
./scripts/benchmark.sh

# Stop all services
./scripts/stop-all.sh
```

#### Service URLs
- Servlet Stack: http://localhost:8081
- WebFlux Stack: http://localhost:8082
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
