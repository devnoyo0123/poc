# JOURNAL — TDD 관찰 일지

각 도메인/사이클별로 Red → Green → Refactor 전환을 기록하고, TDD가 만든 차이점을 회고한다.

---

## 2026-08-10 — 프로젝트 셋업

### 환경 구성
- 빈 Spring Boot 3.3.0 (MVC) + Kotlin 1.9.24 + JDK 21 프로젝트 생성
- PostgreSQL + JPA + QueryDSL (kapt) + Flyway 스택 구성
- 테스트: JUnit5 + Kotest, 테스트 DB는 H2 (PostgreSQL 호환 모드)
- `./gradlew clean build` 빌드 성공 확인

### 관찰
- QueryDSL kapt 설정: Q클래스 생성 디렉토리를 `build/generated/querydsl`로 지정하고 소스셋에 추가. `querydsl.sourceEncoding` 옵션 경고는 무해함.
- 다음 단계: Payout 도메인 스펙 추출 후 첫 Red 사이클 시작.

---

## 2026-08-10 — Login(Auth) 사이클 1: User 도메인 [Red→Green]

### 무엇을 했나
- 로그인 도메인 스펙 추출 (User 엔티티 필드, UserType enum, DTO, 엔드포인트 시그니처 — 구현 코드 읽지 않음)
- User 도메인 모델의 순수 로직을 TDD로 구현

### Red (작성한 테스트)
`UserTest` (8개):
- matchesPassword: 올바른 비밀번호 → true, 틀린 비밀번호 → false
- canLogin: 활성+인증 → true / 비활성 → false / 미인증 → false
- isAdmin / isWriter 역할 확인
- UserType enum 값 (ADMIN, WRITER)

### Green (최소 구현)
- `UserType` enum (ADMIN, WRITER)
- `User` 도메인 클래스 — 스펙의 8개 필드 + `matchesPassword`/`canLogin`/`isAdmin`/`isWriter`

### 관찰 / 인사이트
- **도메인 순수 로직부터 TDD로 잡으니, "유저가 로그인 가능한가?"라는 질문이 `canLogin()`이라는 명확한 메서드로 구체화됐다.** 스펙에는 이 메서드가 없었지만, 테스트를 짜면서 자연스럽게 발명함.
- 비밀번호 매칭을 단순 문자열 비교로 둬서(인코딩값 직접 비교) 테스트가 단순해짐. 실제 PasswordEncoder 결합은 Green 이후 단계로 미룸 — 이게 TDD의 "최소 구현" 원칙.

---

## 2026-08-10 — Login(Auth) 사이클 2: AuthService.login() [Red→Green→Refactor]

### 무엇을 했나
- 로그인 오케스트레이션 서비스를 TDD로 구현
- hexagonal 패턴: AuthService → out-port (UserLoadPort, AccessTokenIssuer, RefreshTokenIssuer)

### Red (작성한 테스트)
`AuthServiceTest` (5개, fake out-port로 메모리 대체):
1. 정상 이메일·비밀번호 → accessToken + refreshToken + userInfo 반환
2. 존재하지 않는 이메일 → AuthException
3. 비밀번호 불일치 → BadCredentialsException (스펙 준수)
4. 비활성 유저 → AuthException
5. 이메일 미인증 유저 → AuthException

### Green (최소 구현)
- `LoginRequest` / `LoginResponse` (스펙 기반 DTO)
- out-port 인터페이스 3개 (UserLoadPort, AccessTokenIssuer, RefreshTokenIssuer)
- `AuthException` (도메인 예외)
- `AuthService.login()` — 4단계 흐름: 조회 → 로그인가능검사 → 비밀번호검증 → 토큰발급

### Refactor
- `!user.isActive || !user.isEmailVerified` → `!user.canLogin()` (사이클1에서 만든 메서드 활용)
- companion object 헬퍼 → 최상위 private 함수로 이동
- 의미 없는 `ex shouldNotBe null` assertion 제거

### 관찰 / 인사이트
- **포트 인터페이스를 먼저 정의하니 AuthService가 영속성/JWT 구현에서 완전히 분리됨.** 테스트는 fake 구현체만으로 전체 로그인 규칙을 검증 — DB나 JWT 라이브러리 없이도.
- **스펙이 "BadCredentialsException을 쓴다"고 하면, 이게 실제로 최선인지 의문이 듦.** 우리 도메인 예외(AuthException)로 통일하는 게 더 일관성 있을 수 있지만, 스펙 충실성을 위해 유지. → 실험 후 회고에서 비교 포인트.
- **spring-security-core만 추가하고 full starter는 피함** — 빈 프로젝트에 SecurityFilterChain 자동설정이 켜지는 걸 막기 위해. TDD가 이런 의존성 범위 결정까지 명확히 만듦.

### 결과
- 총 13개 테스트 통과 (UserTest 8 + AuthServiceTest 5), 0 실패

---

## 2026-08-11 — User password encode 적용 [Red→Green] (tdd-pair 스킬 첫 실행)

### 무엇을 했나
- User 비밀번호 검증을 단순 문자열 비교 → Spring Security `PasswordEncoder` 방식으로 **스펙 정합**

### Red (작성한 테스트)
- `UserSpec` "비밀번호 매칭" 2케이스를 `(raw, encoder)` 시그니처로 교체 + `createForAdmin` 팩토리 사용
- `fakeEncoder` 도입 (결정적 인코더 — 도메인 로직 검증용, 실제 BCrypt는 통합 테스트에서)

### Green (최소 구현)
- `User.matchesPassword(rawPassword: CharSequence, encoder)`: `encoder.matches(raw, password)`
- `companion object createForAdmin(email, rawPassword, name, encoder)`: encode 해서 password 필드에 저장
- `name`/`isActive`/`isEmailVerified`/`userType` 기본값 추가 (기존 테스트 호환 + 스펙 기본값 반영)

### 관찰 / 인사이트
- **스펙 준수가 구현을 이끌었다**: "스펙에 encoder 파라미터가 있네?" → 단순 비교가 아니라 PasswordEncoder 도입이 자연스럽게 결정됨. TDD+스펙 조합이 설계를 강제함.
- **tdd-pair 스킬 첫 실행**: 본인 아이디어("password encode 적용") → 스킬이 Red→Green 자동 실행. 흐름 매끄러움. 다만 `fakeEncoder` 시그니처 실수(`CharSequence` vs `String`)로 1회 재수정 — Java 인터페이스 구현 시 Kotlin 타입 매핑 주의.

### 결과
- 총 7개 테스트 통과 (UserSpec 6 + UserTypeSpec 1), 0 실패

---

## 2026-08-11 — AuthService.login 사이클 1: 정상 로그인 [Red→Green]

### 무엇을 했나
- 로그인 오케스트레이션 서비스 뼈대 + hexagonal 포트 구조 도입 (정상 케이스)

### Red (작성한 테스트)
- `AuthServiceSpec` 1케이스: 정상 이메일·비밀번호 → accessToken/refreshToken/userInfo 반환
- fake 포트 3개(UserLoadPort, AccessTokenIssuer, RefreshTokenIssuer) + NoOp encoder로 도메인 고립

### Green (최소 구현 — 다만 과잉 포함, 아래 관찰 참고)
- `LoginRequest` / `LoginResponse`(+ UserInfo) DTO
- 포트 인터페이스 3개: UserLoadPort, AccessTokenIssuer, RefreshTokenIssuer
- `AuthService.login()`: 조회 → canLogin → matchesPassword → 토큰 발급
- `User`에 `id: Long?` 필드 추가 (토큰 발급용)

### 관찰 / 인사이트
- **과잉 구현 반성**: 정상 케이스 Green인데 스펙 보고 예외 처리(`BadCredentialsException` 3곳)까지 같이 넣었다. 엄밀한 TDD는 "정상 경로만" 최소 구현 후, 다음 Red에서 예외 코드를 넣었어야. 스펙이 명확하다 보니 같이 넣었는데 — 결과적으로 다음 예외 케이스 테스트들이 **이미 통과**할 것. TDD "최소 구현" 원칙을 놓침. 회고 포인트.
- **포트 인터페이스 덕분에 AuthService가 영속성/JWT 구현에서 완전 분리** — 테스트는 fake 포트만으로 전체 로그인 흐름 검증. DB/JWT 라이브러리 불필요.
- **최종 결과 동일**: 원본 스펙(조회→활성→비번→발급) 순서 준수. isEmailVerified는 체크 안 함(원본과 동일).

### 결과
- 총 8개 테스트 통과 (AuthServiceSpec 1 + UserSpec 6 + UserTypeSpec 1), 0 실패

---

<!-- 템플릿
## YYYY-MM-DD — [도메인] [Red|Green|Refactor]

### 무엇을 했나
- 

### Red (작성한 테스트)
- 

### Green (최소 구현)
- 

### 관찰 / 인사이트
- 
-->
