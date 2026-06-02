# S3-SQS LocalStack POC

S3 파일 업로드 → S3 Event Notification → SQS 메시지 수신 흐름을 LocalStack으로 로컬 테스트하는 POC

## 아키텍처

```
파일 업로드 (API) → S3 PutObject → S3 Event Notification → SQS 메시지 자동 발행 → SQS Listener 처리
```

## 기술 스택

- Kotlin + Spring Boot 3.2
- AWS SDK v2 (S3, SQS)
- Spring Cloud AWS (SQS Listener)
- LocalStack 3.5 (S3, SQS 에뮬레이션)
- Testcontainers (통합 테스트)

## 로컬 실행

### 1. LocalStack 시작

```bash
docker-compose up -d
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 파일 업로드 테스트

```bash
curl -X POST http://localhost:8080/api/files/upload \
  -F "file=@test.txt"
```

응답:
```json
{
  "key": "uuid/test.txt",
  "bucket": "file-upload-bucket",
  "size": 13,
  "message": "File uploaded. S3 event notification will be sent to SQS."
}
```

SQS Listener가 이벤트를 수신하여 로그 출력:
```
=== SQS Message Received ===
S3 Event: ObjectCreated:Put
Bucket: file-upload-bucket, Key: uuid/test.txt, Size: 13 bytes
=== Message Processing Complete ===
```

## 통합 테스트

```bash
./gradlew test
```

- Testcontainers로 LocalStack 자동 실행
- S3 업로드 → SQS 메시지 수신 검증
- 단일/다중 파일 업로드 테스트 포함

## 프로젝트 구조

```
src/main/kotlin/com/example/s3sqs/
├── config/
│   └── AwsConfig.kt              # S3, SQS 클라이언트 설정 (LocalStack 엔드포인트 지원)
├── controller/
│   └── FileUploadController.kt   # 파일 업로드 API
├── service/
│   └── S3UploadService.kt        # S3 업로드 로직
├── listener/
│   └── SqsEventListener.kt       # SQS 메시지 수신 리스너
└── dto/
    └── FileUploadResponse.kt     # 응답 DTO

docker-compose.yml                # LocalStack (S3 + SQS + init 스크립트)
localstack/init/setup.sh          # 버킷/큐/이벤트 알림 자동 설정
```

## 핵심 포인트

- **프로덕션 코드 수정 없이** `application.yml` 설정만으로 로컬/AWS 전환
- S3 Event Notification이 자동으로 SQS에 JSON 메시지 발행
- `@SqsListener`로 SQS 메시지 비동기 수신 처리
- Testcontainers로 CI 환경에서도 통합 테스트 가능
