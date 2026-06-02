# Spring Batch Section 2 실습

## 실습 목차

### 1. Tasklet 기반 처리 ✅
- **DeleteOldFilesTasklet**: 오래된 파일 삭제
- ResourcelessTransactionManager 사용

### 2. JobParameters 활용
- 다양한 타입의 파라미터 전달
- @StepScope와 @Value로 주입

### 3. Chunk 기반 처리
- ItemReader/ItemProcessor/ItemWriter 구현
- 대량 데이터 처리

---

## 실습 1: Tasklet 기반 파일 삭제

### 실행 방법

#### 1. 테스트 파일 생성
```bash
./src/main/resources/scripts/setup-test-files.sh
```

#### 2. 배치 실행
```bash
./gradlew bootRun --args='--spring.batch.job.name=deleteOldFilesJob basePath=/tmp/spring-batch-logs daysOld=7'
```

#### 3. 실행 결과 확인
```bash
ls -lh /tmp/spring-batch-logs
```

### 핵심 포인트
- ✅ Tasklet 인터페이스 구현
- ✅ RepeatStatus.FINISHED로 종료
- ✅ JobParameters로 동적 경로/기간 설정
- ✅ ResourcelessTransactionManager (DB 연동 없는 작업)

---

## 실습 2: JobParameters 활용

### 실행 방법

#### 기본 파라미터
```bash
./gradlew bootRun --args='--spring.batch.job.name=parameterDemoJob name=John age=30'
```

#### 날짜 파라미터
```bash
./gradlew bootRun --args='--spring.batch.job.name=parameterDemoJob targetDate=2026-04-13,java.time.LocalDate'
```

#### Enum 파라미터
```bash
./gradlew bootRun --args='--spring.batch.job.name=parameterDemoJob difficulty=EASY,com.example.batch.model.Difficulty'
```

---

## 실습 3: Chunk 기반 처리

### 실행 방법
```bash
./gradlew bootRun --args='--spring.batch.job.name=chunkProcessingJob inputFile=data.csv chunkSize=10'
```

### 처리 흐름
1. ItemReader: 파일에서 데이터 읽기
2. ItemProcessor: 데이터 가공 (null 반환 시 필터링)
3. ItemWriter: chunk size만큼 모아서 DB 저장

---

## 프로젝트 구조

```
src/main/kotlin/com/example/batch/
├── SpringBatchSection2Application.kt
├── config/
│   ├── DeleteOldFilesJobConfig.kt
│   ├── ParameterDemoJobConfig.kt
│   └── ChunkProcessingJobConfig.kt
├── tasklet/
│   ├── DeleteOldFilesTasklet.kt
│   └── ParameterDemoTasklet.kt
├── chunk/
│   ├── reader/
│   ├── processor/
│   └── writer/
└── model/
    └── Difficulty.kt
```
