# JobRunr POC

JobRunr을 사용한 Spring Boot MVC 백그라운드 작업 처리 프로젝트입니다.

## JobRunr 이란?

- Java 백그라운드 작업 처리를 위한 라이브러리
- Database를 사용하여 작업을 영속화
- Web Dashboard 제공
- REST API로 작업 관리 가능
- Retry, Scheduling 등 다양한 기능 제공

## 기술 스택

- Java 21
- Spring Boot 3.3.0
- JobRunr 5.3.3
- H2 Database (In-Memory)
- Gradle 8.8

## 시작하기

### 1. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 2. JobRunr Dashboard 접속

- URL: http://localhost:8000/dashboard
- Username: `admin`
- Password: `admin`

Dashboard에서 다음을 확인할 수 있습니다:
- 대기 중인 작업 (Jobs)
- 처리 중인 작업 (Processing)
- 완료된 작업 (Succeeded)
- 실패한 작업 (Failed)
- 작업 재시도 및 삭제 가능

### 3. REST API 테스트

#### 작업 즉시 실행 (Enqueue)

```bash
curl -X POST "http://localhost:8080/api/jobs/enqueue?message=Hello%20JobRunr"
```

#### 작업 예약 (Schedule)

```bash
curl -X POST "http://localhost:8080/api/jobs/schedule?message=Scheduled%20Job"
```

### 4. H2 Console 접속

- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:jobrunr`
- Username: `sa`
- Password: (비워둠)

JobRunr 테이블들을 확인할 수 있습니다:
- `jobrunr_jobs` - 작업 정보
- `jobrunr_recurring_jobs` - 반복 작업
- `jobrunr_backgroundjobservers` - 백그라운드 작업 서버 정보

## 주요 기능

### SampleJob

`SampleJob.java`는 2초 동안 sleep 후 메시지를 로깅하는 예제 작업입니다.

```java
@Job(name = "Sample Job")
public void execute(String message) {
    log.info("Executing job with message: {}", message);
    // ... 비즈니스 로직
}
```

### JobController

- `POST /api/jobs/enqueue` - 작업을 즉시 실행 큐에 추가
- `POST /api/jobs/schedule` - 작업을 예약

## 추가 기능 실험해보기

### 1. Recurring Jobs (반복 작업)

```java
@RecurringJob(id = "my-recurring-job", cron = "*/5 * * * *")
public void recurringJob() {
    // 5분마다 실행
}
```

### 2. Job失败 및 Retry

```java
@Job(name = "Failable Job")
public void failableJob() {
    throw new RuntimeException("Simulated failure");
}
```

JobRunr가 자동으로 재시도합니다 (기본 설정).

### 3. Job 체이닝

```java
jobScheduler.enqueue(() -> job1())
           .then(() -> job2())
           .then(() -> job3());
```

## 참고 자료

- JobRunr 공식 문서: https://www.jobrunr.io/
- Spring Boot 통합 가이드: https://www.jobrunr.io/en/latest/guide/background-methods/spring-boot/

## Dashboard에서 작업 재시작하기

JobRunr Dashboard는 실패한 작업을 쉽게 재시작할 수 있는 UI를 제공합니다.

### 재시작 단계

1. Dashboard 접속: http://localhost:8000/dashboard (admin/admin)
2. **Failed Jobs** 탭 클릭
3. 실패한 작업 찾기
4. 작업 오른쪽 **"Reenqueue"** 버튼 클릭
5. 작업이 다시 대기열로 들어가서 재실행됨

### Dashboard 기능

- **개별 재시도**: 각 작업별 Reenqueue 버튼
- **일괄 재시도**: 여러 작업 선택 후 한 번에 재시도
- **작업 삭제**: 불필요한 실패 작업 삭제
- **상세 정보**: 작업 파라미터, 로그, 스택 트레이스 확인

### 테스트 방법

```bash
# 1. 실패하는 작업 등록
curl -X POST "http://localhost:8080/api/jobs/failing?message=Test%20Retry"

# 2. Dashboard에서 Failed Jobs 확인
# http://localhost:8000/dashboard

# 3. Reenqueue 버튼으로 재시작
```

### 자동 재시도 vs 수동 재시도

- **자동**: JobRunr가 실패한 작업을 자동으로 재시도 (기본 10회)
- **수동**: Dashboard에서 언제든지 재시작 가능
