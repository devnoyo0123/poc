# Resilience4j Retry + @Transactional POC

## 목적
Spring Retry와 Resilience4j Retry의 차이점을 확인하고, 특히 **retry 동작 시 @Transactional의 커밋/롤백 동작**을 검증하기 위한 POC입니다.

## 환경
- **Spring Boot**: 2.7.3 (ckgr_oms 프로젝트와 동일)
- **JDK**: Java 17
- **Resilience4j**: 1.7.1
- **Database**: H2 in-memory

## 핵심 테스트 시나리오

### 1. ✅ Retry 성공 시 @Transactional 커밋
```java
@Retry(name = "sampleService")
@Transactional
public SampleEntity saveWithRetry(String name, int failUntilAttempt)
```

**시나리오:**
- 1차 시도: RuntimeException 발생 → retry
- 2차 시도: 성공 → **DB에 저장되고 커밋**

**검증:**
- ✅ Retry가 성공하면 트랜잭션이 정상 커밋됨
- ✅ DB에 엔티티가 실제로 저장됨

### 2. ✅ MaxAttempts 초과 시 예외 발생
```java
@Retry(name = "maxRetryService") // maxAttempts: 5
@Transactional
public SampleEntity saveWithMaxRetry(String name)
```

**시나리오:**
- 1~5차 시도: 모두 RuntimeException 발생
- 5번 모두 실패 → **fallback 호출 → RuntimeException 던짐**

**검증:**
- ✅ MaxAttempts 초과 시 예외가 정상적으로 던져짐
- ✅ 모든 트랜잭션이 롤백되어 DB에 아무것도 저장되지 않음

### 3. ✅ Retry 중 트랜잭션 롤백 후 최종 커밋
```java
@Retry(name = "sampleService")
@Transactional
public SampleEntity saveWithRollbackAndRetry(String name, int failUntilAttempt)
```

**시나리오:**
- 1차 시도: DB 저장 후 RuntimeException → **롤백** → retry
- 2차 시도: DB 저장 후 RuntimeException → **롤백** → retry
- 3차 시도: DB 저장 후 성공 → **커밋**

**검증:**
- ✅ 실패한 시도의 트랜잭션은 모두 롤백됨
- ✅ 최종 성공 시에만 트랜잭션이 커밋됨
- ✅ DB에는 3번째 시도의 데이터만 저장됨

## 실행 방법

### 테스트 실행
```bash
cd projects/resilience4j-retry-poc
./gradlew test --info
```

### 특정 테스트만 실행
```bash
./gradlew test --tests RetryTransactionalTest.testRetrySuccessWithTransactionalCommit --info
./gradlew test --tests RetryTransactionalTest.testMaxAttemptsExceededThrowsException --info
./gradlew test --tests RetryTransactionalTest.testRetryWithRollbackAndFinalCommit --info
```

## Spring Retry vs Resilience4j Retry

### Spring Retry
```java
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 100))
@Transactional
public void someMethod() { ... }
```

### Resilience4j Retry
```java
@Retry(name = "serviceName")
@Transactional
public void someMethod() { ... }
```

### 주요 차이점

| 항목 | Spring Retry | Resilience4j |
|------|--------------|--------------|
| 설정 방식 | 어노테이션 기반 | application.yml + 어노테이션 |
| 의존성 | Spring 생태계에 밀접 | 독립적인 라이브러리 |
| 모니터링 | 제한적 | 강력한 메트릭/모니터링 |
| Circuit Breaker | 별도 라이브러리 필요 | 통합 제공 |
| @Transactional 동작 | ✅ 정상 동작 | ✅ 정상 동작 |

## 결론

### ✅ Resilience4j Retry + @Transactional 동작 확인
1. **Retry 성공 시**: 트랜잭션이 정상적으로 커밋됨
2. **MaxAttempts 초과 시**: 예외가 발생하고 모든 트랜잭션 롤백됨
3. **Retry 중 롤백**: 실패한 시도는 롤백되고, 성공한 시도만 커밋됨

### 📝 핵심 발견
- Resilience4j의 `@Retry`는 **Spring의 @Transactional과 완벽하게 호환**됩니다
- Retry 로직이 실행되는 동안 각 시도마다 **새로운 트랜잭션이 생성**됩니다
- 실패한 시도의 트랜잭션은 **자동으로 롤백**되고, 성공한 시도만 **커밋**됩니다

## 로그 분석

테스트 실행 시 다음과 같은 로그를 통해 동작을 확인할 수 있습니다:

```
=== Attempt #1 - saveWithRetry called with name: test-retry-success
Attempt #1 - Throwing RuntimeException (will retry)

=== Attempt #2 - saveWithRetry called with name: test-retry-success
Attempt #2 - Success! Saving entity to DB
Attempt #2 - Entity saved with ID: 1

VERIFICATION:
- Result ID: 1
- Attempt count: 2
- DB count: 1
```

## 다음 단계
- [ ] Circuit Breaker와 Retry 조합 테스트
- [ ] Bulkhead와 Retry 조합 테스트
- [ ] Rate Limiter와 Retry 조합 테스트
- [ ] 메트릭 수집 및 모니터링 구현
