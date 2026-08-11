package com.example.lab.auth.usecase

import com.example.lab.auth.dto.LoginRequest
import com.example.lab.auth.port.AccessTokenIssuer
import com.example.lab.auth.port.RefreshTokenIssuer
import com.example.lab.auth.port.UserLoadPort
import com.example.lab.domain.auth.User
import com.example.lab.domain.auth.UserType
import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeBuilder
import com.navercorp.fixturemonkey.kotlin.set
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder

class LoginUseCaseSpec : DescribeSpec({

    val fixtureMonkey = FixtureMonkey.builder()
        .plugin(KotlinPlugin())
        .build()

    // NoOp encoder — PasswordEncoder는 간단하니 fake 유지 (상태 검증 자연스러움)
    fun noOpEncoder(): PasswordEncoder = object : PasswordEncoder {
        override fun encode(raw: CharSequence): String = raw.toString()
        override fun matches(raw: CharSequence, encoded: String): Boolean = raw.toString() == encoded
    }

    describe("LoginUseCase.login - 정상 로그인") {
        it("올바른 이메일·비밀번호면 accessToken·refreshToken·userInfo 를 반환한다") {
            // given

            val email = "admin@example.com"
            val token = "access-1"
            val expiresIn = 3600
            val refreshToken = "refresh-1"

            val storedUser = User(
                id = 1L,
                email = email,
                password = "password123!",
                name = "관리자",
                isActive = true,
                isEmailVerified = true,
                userType = UserType.ADMIN,
            )
            val userLoadPort = mockk<UserLoadPort>()
            val accessTokenIssuer = mockk<AccessTokenIssuer>()
            val refreshTokenIssuer = mockk<RefreshTokenIssuer>()

            every { userLoadPort.findByEmail(email) } returns storedUser
            every { accessTokenIssuer.issue(storedUser) } returns
                AccessTokenIssuer.TokenIssue(token = token, expiresIn = expiresIn)
            every { refreshTokenIssuer.issue(1L) } returns refreshToken

            val useCase = LoginUseCase(userLoadPort, accessTokenIssuer, refreshTokenIssuer, noOpEncoder())

            // when
            val result = useCase.login(LoginRequest(email = email, password = "password123!"))

            // then
            result.accessToken shouldBe token
            result.tokenType shouldBe "Bearer"
            result.expiresIn shouldBe expiresIn
            result.refreshTokenValue shouldBe refreshToken
            result.userInfo.userId shouldBe "1"
            result.userInfo.userType shouldBe UserType.ADMIN.name
            result.userInfo.name shouldBe "관리자"
            result.userInfo.email shouldBe "admin@example.com"
        }
    }

    describe("LoginUseCase.login - 예외 케이스") {
        it("존재하지 않는 이메일이면 BadCredentialsException 을 던진다") {
            // given: 어떤 이메일로 조회해도 null
            val userLoadPort = mockk<UserLoadPort>()
            val accessTokenIssuer = mockk<AccessTokenIssuer>()
            val refreshTokenIssuer = mockk<RefreshTokenIssuer>()
            every { userLoadPort.findByEmail(any()) } returns null

            val useCase = LoginUseCase(userLoadPort, accessTokenIssuer, refreshTokenIssuer, noOpEncoder())

            // when & then: 예외 발생
            shouldThrow<BadCredentialsException> {
                useCase.login(LoginRequest(email = "nobody@example.com", password = "any"))
            }

            // then: 실패 시 토큰이 발급되지 않아야 한다 (사이드 이펙트 검증)
            verify(exactly = 0) { accessTokenIssuer.issue(any()) }
            verify(exactly = 0) { refreshTokenIssuer.issue(any()) }
        }

        it("비활성 유저(isActive=false)는 BadCredentialsException 을 던진다") {
            // given: 비활성 유저
            val inactiveUser = User(
                id = 1L,
                email = "admin@example.com",
                password = "password123!",
                name = "관리자",
                isActive = false,
                userType = UserType.ADMIN,
            )
            val userLoadPort = mockk<UserLoadPort>()
            val accessTokenIssuer = mockk<AccessTokenIssuer>()
            val refreshTokenIssuer = mockk<RefreshTokenIssuer>()
            every { userLoadPort.findByEmail(any()) } returns inactiveUser

            val useCase = LoginUseCase(userLoadPort, accessTokenIssuer, refreshTokenIssuer, noOpEncoder())

            // when & then: 예외 발생
            shouldThrow<BadCredentialsException> {
                useCase.login(LoginRequest(email = "admin@example.com", password = "password123!"))
            }

            // then: 토큰 발급되지 않음
            verify(exactly = 0) { accessTokenIssuer.issue(any()) }
            verify(exactly = 0) { refreshTokenIssuer.issue(any()) }
        }

        it("비밀번호가 틀리면 BadCredentialsException 을 던진다") {
            // given: 활성 유저지만 비밀번호는 다름
            val user: User = fixtureMonkey.giveMeBuilder<User>()
                .set(User::isActive, true)
                .set(User::email, "admin@example.com")
                .set(User::password, "password123!")
                .sample()

            val userLoadPort = mockk<UserLoadPort>()
            val accessTokenIssuer = mockk<AccessTokenIssuer>()
            val refreshTokenIssuer = mockk<RefreshTokenIssuer>()
            every { userLoadPort.findByEmail(any()) } returns user

            val useCase = LoginUseCase(userLoadPort, accessTokenIssuer, refreshTokenIssuer, noOpEncoder())

            // when & then: 틀린 비밀번호로 예외
            shouldThrow<BadCredentialsException> {
                useCase.login(LoginRequest(email = "admin@example.com", password = "WRONG_PASSWORD"))
            }

            // then: 토큰 발급되지 않음
            verify(exactly = 0) { accessTokenIssuer.issue(any()) }
            verify(exactly = 0) { refreshTokenIssuer.issue(any()) }
        }
    }
})
