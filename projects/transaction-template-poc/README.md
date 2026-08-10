# transaction-template-poc

외부 API 호출 → DB 저장 시나리오에서, **메서드 전체를 `@Transactional`로 감싸면 실패를 잡아도(`catch`) 실패 기록(FAILED)이 롤백되어 사라진다**는 문제와, **트랜잭션 경계를 직접 제어하면 해결된다**는 것을 보여주는 POC.

Gradle 멀티 모듈로 같은 시나리오를 두 스택에서 검증한다.

| 모듈 | 스택 | 트랜잭션 경계 제어 |
|------|------|--------------------|
| `jpa-servlet` | Spring MVC(서블릿) + Spring Data JPA + H2 | `TransactionTemplate` |
| `r2dbc-webflux` | Spring WebFlux + Spring Data R2DBC + R2DBC H2 | `TransactionalOperator` (리액티브 등가물) |

## 핵심 시나리오

`WorkLog(status, detail)` 한 건을 두고:

1. PENDING 으로 저장
2. 외부 API 호출 (실패 가능)
3. 성공 → SUCCESS, 실패 → `catch`에서 FAILED 로 기록하고 싶다

### Broken — 단일 `@Transactional` / 단일 tx

- JPA: 바깥 `@Transactional` 메서드가 또 다른 `@Transactional` 빈(`InnerApiTxService`)을 호출 → 같은 **물리 트랜잭션**에 합류한다. 내부에서 예외가 나면 그 트랜잭션은 **rollback-only** 로 마킹된다. 바깥에서 예외를 잡아 FAILED 를 저장해도, 커밋 시점에 `UnexpectedRollbackException` 이 터지고 **PENDING·FAILED 모두 롤백**된다.
- R2DBC: API 호출·성공 저장·**FAILED 기록 시도까지 하나의 트랜잭션 경계** 안에 두고 최종적으로 에러를 다시 던진다. 경계에서 에러 신호를 받아 **전부 롤백**된다.

→ 결과: 실패 기록이 DB에 안 남는다. (테스트에서 row 0건 확인)

### Fixed — `TransactionTemplate` / `TransactionalOperator`

- 메서드 레벨 `@Transactional` 을 쓰지 않는다.
- **tx #1**: PENDING 저장 + API 호출 + SUCCESS 저장 (all-or-nothing). 실패하면 tx #1 이 롤백되고 PENDING 도 사라진다.
- `catch`(또는 `onErrorResume`)에서 **tx #2** 를 새로 열어 FAILED 를 저장한다. tx #1 과 무관하게 **독립적으로 커밋**된다.

→ 결과: 실패가 FAILED 로 DB에 남는다. (테스트에서 FAILED row 1건 확인)

## 실행

```bash
# 전체 테스트 (두 모듈)
./gradlew test

# 모듈별
./gradlew :jpa-servlet:test
./gradlew :r2dbc-webflux:test

# 앱 실행 (서로 다른 포트)
./gradlew :jpa-servlet:bootRun       # http://localhost:8080
./gradlew :r2dbc-webflux:bootRun     # http://localhost:8081
```

## HTTP 로 직접 확인

```bash
# 실패 케이스 (fail=true 기본값)
curl -XPOST 'http://localhost:8080/broken?fail=true'   # rows 비어있음 / error 발생
curl -XPOST 'http://localhost:8080/fixed?fail=true'    # FAILED row 1건

# 성공 케이스
curl -XPOST 'http://localhost:8080/fixed?fail=false'   # SUCCESS row

# 현재 저장 상태
curl 'http://localhost:8080/logs'
```

`8081` (r2dbc-webflux) 도 동일한 엔드포인트를 제공한다.

## 블랙박스 시나리오 테스트 (진짜 MySQL + WireMock + curl)

코드 테스트(`@SpringBootTest` + H2)는 트랜잭션 배선을 빠르게 검증하지만 H2는
실제 DB가 아니다. 진짜 충실도를 원하면 컨테이너로 진짜 인프라를 띄우고 curl로
검증한다. `scenario/` 가 그 패턴:

```
scenario/
├── docker-compose.test.yml   # MySQL(:3308) + WireMock(:8089)
├── wiremock/mappings/        # /external/ok -> 200, /external/unstable -> 500
├── data/seed.sql             # 스키마 + 사전 데이터 2건
├── scenarios/                # 시나리오 markdown (사람이 읽는 형식)
└── run-scenario.sh           # 실행기
```

```bash
./scenario/run-scenario.sh
```

흐름: 앱 jar 빌드 → 컨테이너 기동 → SQL 시드 → 앱 실행(`scenario` 프로필, :8082)
→ curl 시나리오 → DB 직접 조회로 검증 → teardown.

`scenario` 프로필에서 앱은 H2 대신 **진짜 MySQL**, `ExternalApiClient`는 fake 대신
**진짜 HTTP 호출**(WireMock 스텁)을 쓴다. 외부 API는 SQL로 못 넣으므로 **WireMock**이
그 자리를 채운다 — A→B→C 같은 외부 호출 체인을 흉내 내는 방법이 이거다.

검증 결과:
| 스텝 | 동작 | 결과 |
|------|------|------|
| 1 | `/fixed?fail=true` (WireMock 500) | FAILED row 1건 커밋 ✅ |
| 2 | `/broken?fail=true` (WireMock 500) | row 변화 없음 (rollback-only) ✅ |
| 3 | `/fixed?fail=false` (WireMock 200) | SUCCESS row 커밋 ✅ |

> 포트 충돌 시(다른 컨테이너/앱이 3307·8080 점유) → MySQL :3308, 앱 :8082 사용.

## 왜 단순 `@Transactional` + `catch` 가 안 되나 (핵심)

- 한 메서드 안에서 예외를 잡고 다시 던지지 않으면 사실 롤백은 **안 일어난다**. 사람들이 실제로 겪는 함정은 **중첩 트랜잭션의 rollback-only 마킹**이다: 같은 물리 트랜잭션에 참여한 내부 호출이 예외를 던지면, 바깥에서 잡아도 트랜잭션은 이미 "롤백만 가능" 상태가 되어 커밋이 거부된다.
- 해결책은 **실패 기록을 실패한 트랜잭션 바깥의 별도 트랜잭션에서 커밋**하는 것이고, 그 경계를 명시적으로 잡는 도구가 `TransactionTemplate`(서블릿) / `TransactionalOperator`(리액티브)다.
