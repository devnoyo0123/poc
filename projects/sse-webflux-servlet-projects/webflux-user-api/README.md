# WebFlux User API with R2DBC

Spring WebFlux 기반의 Reactive REST API 예제 프로젝트입니다. R2DBC를 사용하여 비동기/논블로킹 데이터베이스 액세스를 구현했습니다.

## 기술 스택

- **Spring Boot**: 3.2.0
- **Spring WebFlux**: Reactive Web 프레임워크
- **Spring Data R2DBC**: Reactive Database Access
- **Kotlin**: 1.9.20
- **H2**: 인메모리 데이터베이스 (R2DBC)

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/users` | 전체 사용자 조회 |
| GET | `/api/users/{id}` | ID로 사용자 조회 |
| GET | `/api/users/search?username={username}` | 사용명으로 검색 |
| POST | `/api/users` | 사용자 생성 |
| PUT | `/api/users/{id}` | 사용자 수정 |
| DELETE | `/api/users/{id}` | 사용자 삭제 |
| DELETE | `/api/users` | 전체 사용자 삭제 |

## 실행 방법

```bash
cd projects/webflux-user-api
../gradlew bootRun
```

## 예제 요청

### 사용자 생성
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "email": "john@example.com"}'
```

### 전체 사용자 조회
```bash
curl http://localhost:8080/api/users
```

### 사용자 검색
```bash
curl "http://localhost:8080/api/users/search?username=john"
```

### 사용자 수정
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"username": "johnny", "email": "johnny@example.com"}'
```

### 사용자 삭제
```bash
curl -X DELETE http://localhost:8080/api/users/1
```
