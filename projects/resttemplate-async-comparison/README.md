# RestTemplate Async Comparison POC

Java 17 + Spring Boot 2.7.8 환경에서 RestTemplate의 순차 호출 vs CompletableFuture 병렬 호출 성능 비교

## 개요

외부 API 호출 시 응답이 느린 경우, 여러 API를 순차적으로 호출하는 것과 CompletableFuture를 사용하여 병렬로 호출하는 것의 성능 차이를 비교합니다.

## 기술 스택

- Java 17
- Spring Boot 2.7.8
- RestTemplate
- CompletableFuture
- Virtual Thread (Java 21 미리보기 기능, Java 17에서는 Executor로 대체)

## 실행 방법

```bash
# 프로젝트 디렉토리로 이동
cd projects/resttemplate-async-comparison

# Gradle Wrapper 생성 (최초 1회)
gradle wrapper

# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

## API 엔드포인트

### 1. 순차 호출 테스트
```
GET /api/sequential?userIds=1,2,3,4,5
```

### 2. 병렬 호출 테스트 (CompletableFuture)
```
GET /api/parallel?userIds=1,2,3,4,5
```

### 3. 성능 비교 (순차 vs 병렬)
```
GET /api/compare?userIds=1,2,3,4,5
```

## 테스트 예시

```bash
# 5개 API 순차 호출 (약 5초 소요)
curl "http://localhost:8080/api/sequential?userIds=1,2,3,4,5"

# 5개 API 병렬 호출 (약 1초 소요)
curl "http://localhost:8080/api/parallel?userIds=1,2,3,4,5"

# 성능 비교
curl "http://localhost:8080/api/compare?userIds=1,2,3,4,5"
```

## 응답 예시

```json
{
  "sequential": {
    "executionType": "SEQUENTIAL",
    "totalTimeMs": 5012,
    "results": [...],
    "apiCallCount": 5,
    "avgTimePerCallMs": 1002.4
  },
  "parallel": {
    "executionType": "PARALLEL",
    "totalTimeMs": 1008,
    "results": [...],
    "apiCallCount": 5,
    "avgTimePerCallMs": 201.6
  },
  "comparison": {
    "sequentialTimeMs": 5012,
    "parallelTimeMs": 1008,
    "savedTimeMs": 4004,
    "improvementRate": "79.89%",
    "apiCallCount": 5
  }
}
```

## 설정

`application.yml`에서 지연 시간 조절 가능:

```yaml
external-api:
  delay-ms: 1000  # 각 API 호출당 지연 시간 (ms)
```

## 핵심 코드 설명

### 순차 호출
```java
for (Long userId : userIds) {
    UserResponse response = externalApiService.fetchUser(userId);
    results.add(response);
}
// 총 시간 = N * 각 API 응답시간
```

### 병렬 호출 (CompletableFuture)
```java
List<CompletableFuture<UserResponse>> futures = userIds.stream()
    .map(externalApiService::fetchUserAsync)
    .collect(Collectors.toList());

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .join();
// 총 시간 ≈ 가장 느린 API 응답시간
```

## 주의사항

- 실제 환경에서는 외부 API 서버의 부하, 네트워크 상태 등에 따라 결과가 달라질 수 있습니다.
- 너무 많은 병렬 요청은 외부 서버에 부하를 줄 수 있으므로 적절한 스레드 풀 크기를 설정해야 합니다.
