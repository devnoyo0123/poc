# JPA CAPA Scheduler POC

CAPA Scheduler 시스템의 JPA 엔티티 설계 POC 프로젝트

## 프로젝트 개요

이 프로젝트는 창고 마감 정책(Warehouse Cutoff Policy) 관리 시스템의 JPA 엔티티 설계를 검증하는 POC입니다.

### 주요 기능

- **창고 마감 정책 관리**: 매일/주간 반복 패턴으로 마감 시간대 설정
- **마감 요청 이력 관리**: ESM API 호출 이력 추적
- **정책 변경 이력 추적**: 정책 변경사항 히스토리 관리
- **제네릭 기간 표현**: CutOffPeriod로 LocalTime/LocalDateTime 통합 관리

## 프로젝트 구조

```
src/main/java/com/example/jpa/
├── config/
│   ├── RestTemplateConfig.java     # RestTemplate 설정
│   └── SchedulerConfig.java        # @EnableScheduling 설정
├── controller/
│   └── DemoController.java         # REST API 엔드포인트
├── scheduler/
│   └── ExternalApiScheduler.java   # @Scheduled 데모
├── service/
│   └── ExternalApiService.java     # 외부 API 호출 서비스
├── embeddable/
│   └── CutOffPeriod.java          # 제네릭 @Embeddable (시작~종료 기간)
├── entity/
│   ├── WarehouseCutoffPolicy.java  # 창고 마감 정책
│   ├── WarehouseCutoffRequest.java # 마감 요청 이력
│   └── WarehouseCutoffPolicyHistory.java # 정책 변경 이력
├── enums/
│   ├── RepeatType.java             # 반복 유형 (DAILY, WEEKLY)
│   └── RequestStatus.java          # 요청 상태 (SYNCRSLT, ERROCCUR)
└── dto/
    ├── DateRange.java              # 날짜 범위
    ├── DuplicateCheckResult.java   # 중복 체크 결과
    ├── OverlappingRange.java       # 중복 범위 정보
    ├── ManualCallRequest.java      # 수동 호출 요청
    └── ExternalApiResponse.java    # 외부 API 응답
```

## 🎯 @Scheduled 데모

이 프로젝트는 **@Scheduled 애노테이션을 사용한 외부 API 호출 데모**를 포함합니다.

### 주요 기능

1. **자동 스케줄링**
   - 매 30초마다 외부 API 호출 (fixedDelay)
   - 매 1분마다 특정 게시글 조회 (cron)

2. **수동 트리거**
   - REST API를 통한 수동 호출
   - DemoController 엔드포인트 제공

3. **RestTemplate 사용**
   - JSONPlaceholder API 호출
   - 타임아웃 설정 (5초)

**상세 가이드**: [SCHEDULER_DEMO.md](./SCHEDULER_DEMO.md) 참조

### 빠른 시작

```bash
# 애플리케이션 실행
./gradlew bootRun

# API 호출 테스트
curl http://localhost:8080/api/demo/health
curl http://localhost:8080/api/demo/posts
curl -X POST http://localhost:8080/api/demo/trigger
```

---

## 주요 설계 결정사항

### 1. CutOffPeriod 제네릭 @Embeddable

```java
@Embeddable
public class CutOffPeriod<T extends Temporal & Comparable<? super T>> {
    private T startAt;
    private T endAt;
}
```

**장점**:
- ✅ 코드 중복 제거 (startAt/endAt 필드 재사용)
- ✅ 타입 안전성 (제네릭으로 LocalTime/LocalDateTime 구분)
- ✅ 비즈니스 로직 캡슐화 (검증/포함 체크)
- ✅ 유지보수성 향상

**사용 예시**:
```java
// WarehouseCutoffPolicy (LocalTime)
@Embedded
private CutOffPeriod<LocalTime> cutOffPeriod;

// WarehouseCutoffRequest (LocalDateTime)
@Embedded
private CutOffPeriod<LocalDateTime> cutOffPeriod;
```

### 2. Soft Delete with @Where

```java
@Entity
@Where(clause = "deleted_at IS NULL")
public class WarehouseCutoffPolicy {
    private LocalDateTime deletedAt;
}
```

- 모든 쿼리에 `deleted_at IS NULL` 자동 적용
- Repository에서 별도 필터링 메서드 불필요
- `findAll()`, `findByWarehouseId()` 등이 자동으로 삭제되지 않은 데이터만 조회

### 3. WarehouseCutoffRequest 식별자 전략

**비즈니스 키 vs 기본키**:
- **비즈니스 키**: 없음 (자연 키 부재)
- **기본키**: `id` (자동 생성 대체키)
- **조회 조건**: `warehouseId + startAt + endAt`

**왜 대체키를 사용하는가?**:
- 이 테이블은 **ESM API 호출 이력**을 담는 테이블
- API 호출 실패 후 재시도 허용 → 같은 조건으로 여러 번 요청 가능
- 각 호출이 시간순으로 구분되는 **이벤트**
- 도메인에 자연스러운 식별자가 없을 때는 대체키 사용이 표준 패턴

**시나리오 예시**:
```
1차 요청: warehouseId=1, startAt=2026-01-01, endAt=2026-01-31, status=FAILED
2차 요청: warehouseId=1, startAt=2026-01-01, endAt=2026-01-31, status=SUCCESS
```
→ 두 요청은 비즈니스적으로 다른 request (각각 독립적인 이력 레코드)

### 4. Enum 매핑

```java
@Enumerated(EnumType.STRING)
private RepeatType repeatType;
```

- `EnumType.STRING` 사용 (DB에 문자열로 저장)
- `EnumType.ORDINAL` (숫자) 사용하지 않음 → Enum 순서 변경 시 데이터 오류 방지

## 기술 스택

- **Java**: 17
- **Spring Boot**: 2.7.3
- **Spring Data JPA**: 2.7.3
- **Hibernate**: 5.6.x
- **H2 Database**: In-Memory (테스트용)
- **Lombok**: 1.18.30

## 빌드 및 실행

### 컴파일

```bash
./gradlew compileJava
```

### 테스트

```bash
./gradlew test
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)

## DDL 스키마

```sql
-- 창고 마감 정책
CREATE TABLE warehouse_cutoff_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    repeat_type VARCHAR(20) NOT NULL,
    start_at TIME NOT NULL,
    end_at TIME NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL
);

-- 마감 요청 이력
CREATE TABLE warehouse_cutoff_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL
);

-- 정책 변경 이력
CREATE TABLE warehouse_cutoff_policy_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_cutoff_policy_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    repeat_type VARCHAR(20) NOT NULL,
    start_at TIME NOT NULL,
    end_at TIME NOT NULL,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL
);
```

## 검증 체크리스트

- [x] CutOffPeriod 제네릭 @Embeddable 구현
- [x] RepeatType, RequestStatus Enum 구현
- [x] WarehouseCutoffPolicy 엔티티 (@Where 포함)
- [x] WarehouseCutoffRequest 엔티티 (helper methods 포함)
- [x] WarehouseCutoffPolicyHistory 엔티티 (@Where 포함)
- [x] DateRange, DuplicateCheckResult, OverlappingRange, ManualCallRequest DTO
- [x] @Enumerated(EnumType.STRING) 확인
- [x] @PrePersist, @PreUpdate 자동 타임스탬프
- [ ] 컴파일 검증
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성

## 다음 단계

1. **Repository 계층 구현**
   - WarehouseCutoffPolicyRepository
   - WarehouseCutoffRequestRepository
   - WarehouseCutoffPolicyHistoryRepository

2. **서비스 계층 구현**
   - 중복 체크 로직
   - 정책 생성/수정/삭제
   - 이력 관리

3. **테스트 작성**
   - 엔티티 단위 테스트
   - Repository 통합 테스트
   - 비즈니스 로직 테스트

## 참고 문서

- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Hibernate User Guide](https://docs.jboss.org/hibernate/orm/5.6/userguide/html_single/Hibernate_User_Guide.html)
- [JPA 2.2 Specification](https://download.oracle.com/otn-pub/jcp/persistence-2_2-mrel-spec/JavaPersistence.pdf)

## 라이선스

MIT License
