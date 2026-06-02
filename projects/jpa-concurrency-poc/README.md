# JPA Concurrency POC

JPA와 PostgreSQL의 Unique 제약조건을 활용한 동시성 제어 테스트 프로젝트입니다.

## 목적

동시에 같은 데이터로 여러 요청이 들어올 때, **Unique 제약조건을 통해 데이터 중복을 방지**하는 방법을 테스트합니다.

## 기술 스택

- Kotlin 1.9.23
- Spring Boot 3.2.5
- Spring Data JPA
- PostgreSQL 16 (TestContainers)
- JUnit 5
- TestContainers

## 동시성 제어 전략 비교

### Strategy 1: 단순 저장 + Unique 제약조건
```kotlin
// 가장 간단하고 신뢰할 수 있는 방법
@Transactional
fun createUserAccountSimple(...): UserAccount {
    return try {
        repository.save(user)
    } catch (e: DataIntegrityViolationException) {
        // Unique 위반 처리
        throw e
    }
}
```

### Strategy 2: SELECT 후 저장
```kotlin
// Race Condition 가능성 있음
@Transactional
fun createUserAccountWithCheck(...): UserAccount? {
    val existing = repository.findByEmail(email)
    if (existing != null) return null
    return repository.save(user) // 여기서 충돌 가능
}
```

## 테스트 시나리오

### 1. 단일 생성 테스트
- 정상적인 단일 요청이 성공하는지 확인

### 2. 동일 이메일 동시 요청 (10개)
- 10개의 스레드가 동시에 같은 이메일로 생성 시도
- **기대결과**: 1개만 성공, 9개는 DataIntegrityViolationException

### 3. 서로 다른 이메일 동시 요청 (10개)
- 10개의 스레드가 각각 다른 이메일로 생성
- **기대결과**: 모두 성공 (10개 저장)

### 4. Race Condition 테스트
- SELECT 후 INSERT 패턴 테스트
- **기대결과**: Race Condition 발생 가능성 확인

### 5. 스트레스 테스트 (50개 동시 요청)
- 고부하 상황에서도 1개만 저장되는지 확인

## 실행 방법

### 테스트 실행
```bash
./gradlew test
```

### 특정 테스트만 실행
```bash
./gradlew test --tests UserAccountConcurrencyTest
```

### 애플리케이션 실행
```bash
./gradlew bootRun
```

## 엔티티 구조

```kotlin
@Entity
@Table(uniqueConstraints = [
    UniqueConstraint(name = "uk_email", columnNames = ["email"]),
    UniqueConstraint(name = "uk_username", columnNames = ["username"])
])
class UserAccount(
    val email: String,      // Unique
    val username: String,   // Unique
    val fullName: String,
    @Version var version: Long?  // 낙관적 락
)
```

## 핵심 발견사항

1. **Unique 제약조건이 가장 신뢰할 수 있는 동시성 제어 방법**
   - DB 레벨에서 중복을 방지
   - 트랜잭션 격리 수준과 무관하게 동작

2. **SELECT 후 INSERT는 Race Condition 발생 가능**
   - SELECT와 INSERT 사이에 다른 트랜잭션이 INSERT 가능
   - 애플리케이션 레벨 체크만으로는 부족함

3. **낙관적 락(@Version)은 업데이트에만 유효**
   - INSERT 시에는 충돌 방지에 도움이 안 됨
   - Unique 제약조건이 필수

4. **TestContainers로 통합 테스트 가능**
   - 실제 PostgreSQL 환경에서 테스트
   - 컨테이너 재사용으로 빠른 테스트 실행

## API 예시

### 사용자 생성 (Simple)
```bash
curl -X POST "http://localhost:8080/api/users/simple?email=test@example.com&username=testuser&fullName=Test%20User"
```

### 사용자 생성 (With Check)
```bash
curl -X POST "http://localhost:8080/api/users/check?email=test@example.com&username=testuser&fullName=Test%20User"
```

### 통계 조회
```bash
curl "http://localhost:8080/api/users/stats?email=test@example.com"
```

## 학습 포인트

✅ Unique 제약조건이 동시성 제어의 핵심
✅ DB 제약조건을 애플리케이션 레벨에서 우아하게 처리
✅ TestContainers로 실제 환경과 유사한 테스트 가능
✅ 동시성 버그를 재현하고 테스트하는 방법
