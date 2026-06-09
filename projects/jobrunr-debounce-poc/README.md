# JobRunr Debounce POC

JobRunr `schedule()` + `deletePermanently()` 를 이용한 **Job 디바운싱 컨셉 증명** 프로젝트.

## 목적

정산 엑셀 업로드 후 30분 뒤 집계 Job을 실행할 때, 같은 월에 여러 번 업로드되면 **마지막 업로드 후에만 1회 실행**되도록 디바운싱하는 메커니즘을 증명한다.

## 기술 스택

- Kotlin, JDK 21
- Spring Boot 3.3 (WebFlux + Web)
- JobRunr 7.3.2
- PostgreSQL (Docker Compose)
- HikariCP (JDBC Connection Pool)

## 핵심 발견

### JobRunr 디바운싱 방식 비교

| 방식 | 결과 | 이유 |
|------|------|------|
| 결정론적 UUID + `delete()` | ❌ 실패 | `delete()`는 소프트 삭제(DELETED 상태), 같은 ID로 `schedule()` 불가 |
| 랜덤 UUID + ConcurrentHashMap | ✅ 동작 | 매번 새 UUID, Map으로 이전 Job 추적. 싱글 인스턴스에서만 가능 |
| 결정론적 UUID + `deletePermanently()` | ✅ 동작 | **하드 삭제로 row 완전 제거**, 같은 ID로 재생성 가능 |

### 채택한 방식

```
val jobId = UUID.nameUUIDFromBytes("upload-settlement-$key".toByteArray())

// 1. 이전 Job 완전 삭제
storageProvider.deletePermanently(jobId)

// 2. 같은 ID로 새 schedule 생성
jobRequestScheduler.schedule(jobId, runAt, DebounceJobRequest(key))
```

**별도 추적 테이블 없이 job ID 자체가 트래킹 수단.**

### 디바운스 동작 검증

```
10:00  Upload 1 → schedule(jobId, 10:10)
10:03  Upload 2 → deletePermanently(jobId) → schedule(jobId, 10:13)
10:06  Upload 3 → deletePermanently(jobId) → schedule(jobId, 10:16)
10:16  Job 실행 (1회)
```

## 실행 방법

### 1. PostgreSQL 시작

```bash
docker compose up -d
```

### 2. 앱 실행

```bash
./gradlew bootRun
```

### 3. JobRunr Dashboard

http://localhost:8000/dashboard

### 4. 디바운스 테스트

```bash
# 같은 key로 3번 트리거 (3초 간격)
./test-debounce.sh 2024-01 3 3

# 또는 수동
curl -X POST localhost:8080/api/trigger/2024-01
curl -X POST localhost:8080/api/trigger/2024-01
curl -X POST localhost:8080/api/trigger/2024-01

# 약 10초 후 로그에서 [JOB EXECUTED] 1회만 확인
```

## JobRunr 학습 포인트

- `schedule(UUID id, ...)` — 같은 ID가 존재하면 **무시** (덮어쓰기 아님)
- `delete(UUID)` — **소프트 삭제** (state = DELETED, row 남음)
- `deletePermanently(UUID)` — **하드 삭제** (row 제거, StorageProvider 필요)
- Label은 Dashboard 표시 전용, **쿼리 불가**
- `JobRunrMetadata` — KV 스토어 제공하지만 디바운스엔 불필요
- Dashboard는 Servlet 기반 → `spring-boot-starter-web` 필요
