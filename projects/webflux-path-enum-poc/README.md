# webflux-path-enum-poc

**Path Enumeration 방식의 무한 depth 댓글 시스템 PoC**
Spring Boot WebFlux + jOOQ + R2DBC + Kotlin Coroutines

> 학습 출처: "스프링부트로 직접 만들면서 배우는 대규모 시스템 설계 — 게시판" 강의 **Part 2 Ch3 Section 21**

---

## 목적

Path Enumeration(경로 열거) 모델로 **무한 depth 계층형 댓글**을 설계/구현하며,
비동기 스택(WebFlux + R2DBC + Coroutines)에서 jOOQ 를 "타입 안전 SQL 빌더"로 활용하는 패턴을 익힌다.

## 핵심 설계: path enumeration + base62

- 각 댓글은 자신까지의 경로를 문자열로 저장: `path VARCHAR(255) COLLATE utf8mb4_bin`
- 경로는 노드 ID(snowflake) 를 **base62 5자리 고정폭** 으로 인코딩한 세그먼트들의 결합.
  - 예) 루트 1 → `00001`, 루트 1의 자식 7 → `0000100007`
- depth = `path.length() / 5`
- 핵심 질의:
  - **자손 전체**: `WHERE post_id = ? AND path LIKE CONCAT(?, '%')` (prefix = 부모 path)
  - **직계 자식**: depth 와 결합
- **왜 `utf8mb4_bin` 인가**: base62 문자(`0-9A-Za-z`)의 정렬/비교가 ASCII 코드 포인트 순서와
  정확히 일치해야 한다. case-insensitive collation 을 쓰면 `a` 와 `A` 가 동일하게 취급되어 경로가 꼬인다.

## 기술 스택

| 영역 | 기술 | 버전 |
|------|------|------|
| Runtime | Java (Zulu) | 21 LTS |
| Framework | Spring Boot | 3.4.1 |
| Language | Kotlin | 2.0.21 |
| SQL DSL | jOOQ (공식 Gradle plugin) | 3.20.16 |
| Reactive DB | R2DBC (asyncer r2dbc-mysql) + pool | Spring Boot BOM |
| Migration | Flyway (core + mysql) | Spring Boot BOM |
| Coroutines | kotlinx-coroutines-reactive/reactor/core | 1.x |
| Build | Gradle (Kotlin DSL) | 8.13 |
| Test | Testcontainers (mysql / r2dbc) | Spring Boot BOM |

## Phase 구성

| Phase | 범위 | 상태 |
|-------|------|------|
| **1 (현재)** | 스켈레톤 + jOOQ codegen 검증 (generated 클래스 생성) | ✅ |
| 2 | Repository / Service / Controller (WebFlux + Coroutines) | 예정 |
| 3 | 통합 테스트 (Testcontainers MySQL + R2DBC) | 예정 |

## 실행 / 검증 (Phase 1)

### 1. MySQL 컨테이너 기동
```bash
docker compose up -d
```

### 2. 스키마 적용 (codegen 용)
```bash
# Flyway 는 앱 부팅 시 자동 실행되지만, codegen 은 "라이브 스키마 introspect" 이므로
# codegen 직전에 스키마를 DB 에 올려둬야 한다. 간단히 마이그레이션 SQL 을 직적 주입.
docker exec -i path-enum-mysql mysql -uroot -proot path_enum \
  < src/main/resources/db/migration/V1__init_comments.sql
```

### 3. jOOQ codegen → generated 클래스 생성
```bash
./gradlew jooqCodegenJooq
# 산출물: build/generated-jooq/src/main/java/com/career/pathenum/generated/
```

### 4. 컴파일 검증
```bash
./gradlew compileKotlin
```

> ⚠️ Phase 1 에서는 `bootRun`/실제 앱 실행은 하지 않는다 (MUST NOT).

## 프로젝트 구조

```
webflux-path-enum-poc/
├── build.gradle.kts                  # Spring Boot + jOOQ 공식 plugin + R2DBC + Flyway
├── settings.gradle.kts
├── docker-compose.yml                # MySQL 8 (utf8mb4 / utf8mb4_bin 기본)
├── README.md
└── src/main/
    ├── kotlin/com/career/pathenum/
    │   ├── PathEnumApplication.kt    # @SpringBootApplication 진입점
    │   ├── config/
    │   │   └── JooqConfig.kt         # DSLContext (SQL 빌더 전용, MySQL 방언)
    │   ├── model/
    │   │   ├── Comment.kt            # 도메인 모델
    │   │   └── dto/CommentDtos.kt    # Request/Response/TreeNode DTO
    │   └── util/
    │       └── Base62Encoder.kt      # Snowflake ID ↔ base62 5자리 고정폭
    └── resources/
        ├── application.yml           # R2DBC + Flyway(JDBC URL) + jOOQ DEBUG
        └── db/migration/
            └── V1__init_comments.sql  # comments 테이블 (utf8mb4_bin)
```

`build/generated-jooq/` 는 codegen 산출물로 VCS 에 넣지 않는다 (.gitignore).

## 학습 포인트

1. **path enumeration vs adjacency list vs nested set** 의 trade-off
2. base62 고정폭 인코딩으로 인한 **LIKE prefix scan** 의 단순함
3. `utf8mb4_bin` collation 이 정렬 보장에 필수적인 이유
4. WebFlux/R2DBC 환경에서 **jOOQ 를 실행기가 아닌 SQL 빌더로** 사용하는 실용 패턴

## 참고

- jOOQ Gradle plugin: https://www.jooq.org/doc/3.20/manual/code-generation/codegen-execution/codegen-gradle/
- R2DBC MySQL (asyncer): https://github.com/asyncer-io/r2dbc-mysql
