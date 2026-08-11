# SPEC — 블랙박스 명세

> ⚠️ 이 파일에는 **스펙(계약)**만 기록한다.
> 엔티티 필드, DTO 필드, 엔드포인트 시그니처, enum 값.
> **구현 알고리즘/계산식/비즈니스 로직은 절대 기재하지 않는다.**

참조 원본: 기존 프로덕션 백엔드 (구현 코드는 읽지 않음 — 선언부 시그니처만).

---

## Auth(Login) 도메인

### 엔티티: User
테이블: `users` (unique constraint `uk_users_01` on `email`)

| 필드 | 타입 | 컬럼 | 비고 |
|------|------|------|------|
| email | String | email | unique, length=254 |
| password | String | password | 인코딩된 값 |
| name | String | name | not null, length=100 |
| userType | UserType | user_type | not null, @Enumerated(STRING) |
| isActive | Boolean | is_active | default true |
| isEmailVerified | Boolean | is_email_verified | default false |
| isManaged | Boolean | is_managed | default true |
| timezone | String | timezone | not null, length=64, default "Asia/Seoul" |

### 엔티티: RefreshToken
테이블: `refresh_tokens`

| 필드 | 타입 | 컬럼 | 비고 |
|------|------|------|------|
| userId | Long | user_id | not null |
| token | String | token | not null, length=255 |
| expiresAt | Instant | expires_at | not null |
| revokedAt | Instant? | revoked_at | nullable |

### Enum: UserType
- `ADMIN`
- `WRITER`

### DTO: LoginRequest
```
record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
)
```

### DTO: LoginResponse
```
record LoginResponse(
    String accessToken,
    String tokenType,
    int expiresIn,
    UserInfo userInfo,
    String refreshTokenValue
) {
    record UserInfo(
        String userId,
        String userType,
        String name,
        String email
    )
}
```

### REST 엔드포인트 — `AuthController` (`/api/v1/auth`)
| 메서드 | 경로 | 시그니처 |
|--------|------|------|
| POST | `/login` | `login(LoginRequest, HttpServletResponse) → ApiResponse<LoginResponse>` |
| POST | `/refresh` | `refresh(@CookieValue refreshToken, HttpServletResponse) → ApiResponse<LoginResponse>` |
| GET | `/me` | `me(@AuthenticationPrincipal) → ApiResponse<UserInfo>` |
| POST | `/logout` | `logout(@AuthenticationPrincipal, HttpServletResponse) → ApiResponse<String>` |
| POST | `/reset-password` | `resetPassword(ResetPasswordRequest) → ApiResponse<Void>` |
| DELETE | `/users/{userId}` | `deleteUser(userId, @AuthenticationPrincipal) → ApiResponse<Void>` |
| GET | `/verify-activation` | `verifyActivation(@RequestParam token) → ApiResponse<VerifyActivationResponse>` |
| POST | `/activate` | `activate(ActivateRequest) → ApiResponse<String>` |

### 에러 시그널링
- `BadCredentialsException` (Spring Security) → HTTP 401
- `BusinessException(AuthError)` → 메시지 키 기반

### AuthError 메시지 키 (인증 관련)
- `USER_NOT_FOUND` → `error.auth.user.not_found`
- `EMAIL_NOT_FOUND` → `error.auth.reset_password.email_not_found`
- `INVALID_TOKEN` / `TOKEN_EXPIRED` / `TOKEN_ALREADY_USED`
- `PASSWORD_REQUIRED` / `PASSWORD_TOO_SHORT` / `PASSWORD_TOO_LONG`
- `EMAIL_REQUIRED` / `EMAIL_TOO_LONG` / `EMAIL_INVALID_FORMAT`

### RefreshToken 쿠키
- 이름: `refreshToken`
- httpOnly, SameSite=Strict, path `/api/v1/auth`, max age 14일

---

## RefreshToken 도메인

### 엔티티: RefreshToken
테이블: `refresh_tokens`

| 필드 | 타입 | 컬럼 | 비고 |
|------|------|------|------|
| userId | Long | user_id | not null |
| token | String | token | not null, length=255 |
| expiresAt | Instant | expires_at | not null |
| revokedAt | Instant? | revoked_at | nullable, default null |

### 팩토리 / 동작 시그니처 (선언만 — 구현은 본인이)
- `companion object fun create(userId: Long, expirationDays: Long = 14): RefreshToken`
- `fun isExpired(): Boolean`
- `fun isRevoked(): Boolean`
- `fun isValid(): Boolean`
- `fun revoke()`

### out-port: RefreshTokenPort
```
fun createRefreshToken(userId: Long): RefreshToken
fun validateAndFindToken(tokenValue: String): RefreshToken?
fun revokeToken(tokenValue: String): Boolean
fun revokeAllTokensByUserId(userId: Long)
fun rotateToken(oldTokenValue: String): RefreshToken
```

### 비고 (스펙 기반)
- 토큰 문자열 자체는 어떻게 생성하든 자유 (구현 로직 — 본인이 결정)
- 만료 기본값: 14일
- `isValid()` = 만료 안 됨 + revoke 안 됨
- 쿠키로 전달됨 (httpOnly, path `/api/v1/auth`, 14일 max age)

---

## Contract 도메인 (예정)

### Enum: ContractStatus
- `ACTIVE` (0)
- `EXPIRED` (1)
- `EXPIRING_SOON` (2)

(상세 필드는 해당 단계 진입 시 추출)

---

## Payout / Settlement / Aggregation 도메인 (예정)

상세 스펙은 해당 단계 진입 시 추출.
