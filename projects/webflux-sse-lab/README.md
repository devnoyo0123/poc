# WebFlux + SSE 실습 랩

Spring WebFlux와 Kotlin Coroutines을 사용한 SSE (Server-Sent Events) 실습 프로젝트

## 🎯 학습 목표

1. **WebFlux 기초**
   - Spring WebFlux 설정
   - Reactive Streams (Flux, Mono)
   - Kotlin Coroutines 통합

2. **SSE (Server-Sent Events) 마스터**
   - 기본 SSE 구현
   - 객체 전송
   - 실시간 데이터 스트리밍

3. **suspend 함수 vs Flux 이해**
   - 언제 suspend를 쓰고 언제 Flux를 쓰는지
   - 각각의 장단점

4. **실무 패턴**
   - 주식 가격 스트리밍
   - 실시간 알림
   - REST API + SSE 혼합

---

## 🚀 빠른 시작

### 1. 의존성 설치

```bash
./gradlew build
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 테스트

#### SSE 테스트

```bash
# 기본 알림 스트리밍
curl -N http://localhost:8080/api/sse/notifications

# 객체 스트리밍
curl -N http://localhost:8080/api/sse/notifications/object

# 주식 가격 스트리밍
curl -N http://localhost:8080/api/sse/stock/AAPL
```

#### REST API 테스트

```bash
# 단건 조회
curl http://localhost:8080/api/users/1

# 목록 조회
curl http://localhost:8080/api/users

# 사용자 생성
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"id":4,"name":"Alice","email":"alice@example.com"}'
```

---

## 🗄️ R2DBC 관계 매핑 실습

WebFlux + R2DBC로 다양한 조인을 구현해보세요!

### 설정

```bash
# PostgreSQL 실행 (Docker)
docker run --name postgres-r2dbc \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=webflux_lab \
  -p 5432:5432 \
  -v $(pwd)/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/schema.sql \
  -d postgres:16-alpine

# 스키마 수동 실행 (필요시)
docker exec -i postgres-r2dbc psql -U postgres -d webflux_lab < src/main/resources/schema.sql
```

### 조인 유형별 API

#### 1. One-to-One (1:1)
```bash
# 사용자와 부서 조회
curl http://localhost:8080/api/relationships/users/1/with-department

# 부서와 담당자 조회
curl http://localhost:8080/api/relationships/departments/1/with-manager
```

#### 2. Many-to-One (N:1)
```bash
# 게시글과 작성자 조회
curl http://localhost:8080/api/relationships/posts/1/with-author

# 모든 게시글과 작성자
curl http://localhost:8080/api/relationships/posts/all-with-author
```

#### 3. One-to-Many (1:N)
```bash
# 사용자와 게시글들
curl http://localhost:8080/api/relationships/users/1/with-posts

# Single Query 방식
curl http://localhost:8080/api/relationships/users/1/with-posts-single
```

#### 4. Many-to-Many (N:M)
```bash
# 게시글과 태그
curl http://localhost:8080/api/relationships/posts/1/with-tags

# 태그와 게시글들
curl http://localhost:8080/api/relationships/tags/1/with-posts
```

**자세한 내용:** [R2DBC_GUIDE.md](./R2DBC_GUIDE.md)

---

## 📚 실습 가이드

### 실습 1: 기본 SSE

**파일:** `SseController.notifications()`

**목표:**
- 1초마다 알림 전송
- SSE 포맷 이해

**실행:**
```bash
curl -N http://localhost:8080/api/sse/notifications
```

**예상 출력:**
```
id: 0
event: notification
data: 알림 #0 - 2026-05-25T10:30:00

id: 1
event: notification
data: 알림 #1 - 2026-05-25T10:30:01
```

---

### 실습 2: 객체 SSE

**파일:** `SseController.notificationObjects()`

**목표:**
- JSON 객체를 SSE로 전송
- 자동 직렬화 이해

**실행:**
```bash
curl -N http://localhost:8080/api/sse/notifications/object
```

**예상 출력:**
```
id: 1
event: notification
data: {"id":1,"message":"새 알림 #1","timestamp":"2026-05-25T10:30:00"}

id: 2
event: notification
data: {"id":2,"message":"새 알림 #2","timestamp":"2026-05-25T10:30:02"}
```

---

### 실습 3: 주식 가격 스트리밍

**파일:** `SseController.stockPrice()`

**목표:**
- 실시간 데이터 업데이트
- 500ms마다 가격 변경

**실행:**
```bash
curl -N http://localhost:8080/api/sse/stock/AAPL
```

**예상 출력:**
```
id: AAPL-2026-05-25T10:30:00
event: price-update
data: {"symbol":"AAPL","price":152.34,"change":2.34,"timestamp":"..."}

id: AAPL-2026-05-25T10:30:00.500
event: price-update
data: {"symbol":"AAPL","price":148.90,"change":-1.10,"timestamp":"..."}
```

---

### 실습 4: 일반 REST API

**파일:** `UserController.getUser()`

**목표:**
- suspend 함수로 REST API 구현
- 단일 값 반환 이해

**실행:**
```bash
curl http://localhost:8080/api/users/1
```

**예상 출력:**
```json
{"id":1,"name":"John Doe","email":"john@example.com"}
```

---

## 🔍 핵심 개념

### suspend 함수 vs Flux

| | suspend 함수 | Flux |
|---|-------------|------|
| **용도** | 단일 값 반환 | 여러 값 스트리밍 |
| **반환** | `suspend T` | `Flux<T>` |
| **사용처** | 일반 REST API | SSE, WebSocket |
| **예시** | `suspend fun getUser(): User` | `fun stream(): Flux<User>` |

### SSE 포맷

```
id: 1
event: notification
data: {"message":"Hello"}

retry: 3000
```

- **id**: 이벤트 ID
- **event**: 이벤트 타입
- **data**: 실제 데이터
- **retry**: 재접속 지연 (ms)

---

## 📊 코드 구조

```
src/main/kotlin/com/example/webfluxsselab/
├── WebFluxSseLabApplication.kt  # 메인
├── controller/
│   ├── SseController.kt          # SSE 예제
│   └── UserController.kt          # REST API 예제
├── service/
│   └── UserService.kt
└── model/
    └── Models.kt
```

---

## 💡 추가 학습

### 1. 클라이언트에서 SSE 받기 (JavaScript)

```javascript
const eventSource = new EventSource('http://localhost:8080/api/sse/notifications');

eventSource.addEventListener('notification', (event) => {
    const data = event.data;
    console.log('알림:', data);
});

eventSource.onerror = (error) => {
    console.error('SSE 에러:', error);
};
```

### 2. WebClient로 외부 API 호출

```kotlin
val webClient = WebClient.create("https://api.github.com")

val repos = webClient.get()
    .uri("/users/{username}/repos", "octocat")
    .retrieve()
    .awaitBody<List<Repo>>()
```

### 3. DB 연결 (R2DBC)

```kotlin
@Repository
class UserRepository(private val template: R2dbcEntityTemplate) {

    suspend fun findById(id: Long): User? {
        return template.selectOne(
            Query.query(Criteria.where("id").isEqualTo(id)),
            User::class.java
        )
    }
}
```

---

## 🎓 면접 준비

### Q: suspend 함수와 Flux의 차이는?

**A:**
- `suspend 함수`: 단일 값 반환 (REST API)
- `Flux`: 여러 값 스트리밍 (SSE, WebSocket)
- 성능은 동일, 문법 차이

### Q: 언제 SSE를 써야 하나요?

**A:**
- 서버 → 클라이언트 단방향 스트리밍
- 실시간 알림, 주식 가격
- WebSocket보다 가벼움

### Q: WebFlux는 언제 쓰나요?

**A:**
- 대량의 I/O 작업 (DB, API)
- 높은 동시성 필요
- 실시간 스트리밍

---

## 📖 참고 자료

- [Spring WebFlux 공식 문서](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)
- [Server-Sent Events (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)

---

**생성일:** 2026-05-25
**난이도:** ⭐⭐⭐ (중급)
**예상 시간:** 2-3시간
