# API Call Retry & Save POC

외부 API 호출 시 재시도(Retry) 로직을 적용하고, 성공/실패 여부와 상관없이 결과를 데이터베이스에 저장하는 POC 프로젝트입니다.

## 특징

- ✅ 외부 API 호출 (RestTemplate)
- ✅ Resilience4j를 활용한 재시도 메커니즘
- ✅ 5xx (Internal Server Error) 에러 시 자동 재시도
- ✅ 429 (Too Many Requests) 에러 시 자동 재시도
- ✅ 성공/실패 모두 데이터베이스에 저장
- ✅ 호출 이력 조회 기능

## 기술 스택

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- Resilience4j 2.1.0
- H2 Database (In-Memory)
- Lombok

## 아키텍처

```
Controller → Service (with @Retry) → External API
                    ↓
               Repository (Save Results)
```

## 재시도 설정 (application.yml)

```yaml
resilience4j:
  retry:
    instances:
      apiCallRetry:
        max-attempts: 3              # 최대 재시도 횟수
        wait-duration: 1s             # 재시도 대기 시간
        exponential-backoff-multiplier: 2  # 지수 백오프
        retry-exceptions:              # 재시도할 예외
          - org.springframework.web.client.HttpServerErrorException
          - org.springframework.web.client.HttpClientErrorException$TooManyRequests
```

## 실행 방법

### 1. 애플리케이션 시작

```bash
cd api-call-retry-save-poc
mvn spring-boot:run
```

### 2. API 호출 테스트

#### 성공 시나리오 - JSONPlaceholder API
```bash
curl -X POST http://localhost:8080/api/call \
  -H "Content-Type: application/json" \
  -d '{"url": "https://jsonplaceholder.typicode.com/posts/1"}'
```

또는 GET 방식:
```bash
curl "http://localhost:8080/api/call?url=https://jsonplaceholder.typicode.com/posts/1"
```

#### 500 에러 시나리오 - httpstat (테스트용)
```bash
curl -X POST http://localhost:8080/api/call \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpstat.us/500"}'
```

#### 429 에러 시나리오 - Rate Limit
```bash
curl -X POST http://localhost:8080/api/call \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpstat.us/429"}'
```

### 3. 호출 이력 조회

```bash
curl "http://localhost:8080/api/call/history?endpoint=https://jsonplaceholder.typicode.com/posts/1"
```

### 4. H2 Console

데이터베이스 확인:
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: `password`

## 응답 예시

### 성공 응답
```json
{
  "id": 1,
  "endpoint": "https://jsonplaceholder.typicode.com/posts/1",
  "statusCode": 200,
  "status": "SUCCESS",
  "responseBody": "{...}",
  "errorMessage": null,
  "attemptCount": 1,
  "isSuccess": true,
  "callTime": "2026-02-28T10:30:00",
  "createdAt": "2026-02-28T10:30:00",
  "updatedAt": "2026-02-28T10:30:00"
}
```

### 실패 응답 (재시도 후 실패)
```json
{
  "id": 2,
  "endpoint": "https://httpstat.us/500",
  "statusCode": 0,
  "status": "FAILED",
  "responseBody": null,
  "errorMessage": "Retry limit exceeded: 500 Internal Server Error",
  "attemptCount": 3,
  "isSuccess": false,
  "callTime": "2026-02-28T10:35:00",
  "createdAt": "2026-02-28T10:35:00",
  "updatedAt": "2026-02-28T10:35:00"
}
```

## 로그 예시

```
INFO  - Calling API: https://jsonplaceholder.typicode.com/posts/1
INFO  - API call successful. Saved result ID: 1

INFO  - Calling API: https://httpstat.us/500
ERROR - Server error (5xx) occurred: 500 INTERNAL_SERVER_ERROR
INFO  - Retry attempt #1 for URL: ...
INFO  - Retry attempt #2 for URL: ...
ERROR - All retry attempts failed for URL: ...
INFO  - Failure result saved. ID: 2, Attempts: 3
```

## 데이터베이스 스키마

```sql
CREATE TABLE api_call_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    endpoint VARCHAR(500) NOT NULL,
    status_code INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    response_body VARCHAR(2000),
    error_message VARCHAR(1000),
    attempt_count INT NOT NULL,
    is_success BOOLEAN NOT NULL,
    call_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## 테스트 시나리오

### 1. 정상 API 호출
- URL: https://jsonplaceholder.typicode.com/posts/1
- 예상: 성공 (200 OK), 1회 호출

### 2. 500 에러 (재시도 필요)
- URL: https://httpstat.us/500
- 예상: 3회 시도 후 실패 결과 저장

### 3. 429 에러 (Rate Limit)
- URL: https://httpstat.us/429
- 예상: 3회 시도 후 실패 결과 저장

### 4. 404 에러 (재시도 안함)
- URL: https://jsonplaceholder.typicode.com/notfound
- 예상: 1회 실패 후 저장 (재시도 대상 아님)

## 확장 가능성

1. **WebClient로 마이그레이션**: RestTemplate → WebClient (비동기 지원)
2. **Circuit Breaker 추가**: Resilience4j CircuitBreaker
3. **Rate Limiter 추가**: 외부 API 호출 제한
4. **메트릭 수집**: Micrometer + Prometheus
5. **배치 작업**: Scheduler로 주기적 API 호출
