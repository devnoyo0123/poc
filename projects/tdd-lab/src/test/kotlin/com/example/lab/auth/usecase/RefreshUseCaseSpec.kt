package com.example.lab.auth.usecase

import com.example.lab.auth.port.AccessTokenIssuer
import com.example.lab.auth.port.RefreshTokenIssuer
import com.example.lab.auth.port.RefreshTokenLoadPort
import com.example.lab.auth.port.UserLoadPort
import com.example.lab.domain.auth.RefreshToken
import com.example.lab.domain.auth.User
import com.example.lab.domain.auth.UserType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.security.authentication.BadCredentialsException
import java.time.Instant

class RefreshUseCaseSpec : DescribeSpec({

    describe("RefreshUseCase.refresh - 정상 재발급") {
        it("유효한 refreshToken 으로 새 accessToken·refreshToken 을 발급한다") {
            // given: 저장된 유효한 토큰 + 정상 유저
            val user = User(
                id = 1L,
                email = "admin@example.com",
                password = "password123!",
                name = "관리자",
                isActive = true,
                userType = UserType.ADMIN,
            )
            val token = RefreshToken.create(userId = 1L)   // 유효한 토큰 (14일 뒤 만료)

            val refreshTokenLoadPort = mockk<RefreshTokenLoadPort>()
            val userLoadPort = mockk<UserLoadPort>()
            val accessTokenIssuer = mockk<AccessTokenIssuer>()
            val refreshTokenIssuer = mockk<RefreshTokenIssuer>()

            every { refreshTokenLoadPort.findByTokenValue(token.token) } returns token
            every { userLoadPort.findById(1L) } returns user
            every { accessTokenIssuer.issue(user) } returns
                AccessTokenIssuer.TokenIssue(token = "new-access", expiresIn = 3600)
            every { refreshTokenIssuer.issue(1L) } returns "new-refresh"

            val useCase = RefreshUseCase(refreshTokenLoadPort, userLoadPort, accessTokenIssuer, refreshTokenIssuer)

            // when
            val result: RefreshResult = useCase.refresh(token.token)

            // then
            result.accessToken shouldBe "new-access"
            result.tokenType shouldBe "Bearer"
            result.expiresIn shouldBe 3600
            result.refreshTokenValue shouldBe "new-refresh"
        }
    }

    describe("RefreshUseCase.refresh - 예외 케이스") {
        it("존재하지 않는 토큰이면 BadCredentialsException 을 던진다") {
            val refreshTokenLoadPort = mockk<RefreshTokenLoadPort>()
            every { refreshTokenLoadPort.findByTokenValue(any()) } returns null
            val useCase = RefreshUseCase(refreshTokenLoadPort, mockk(), mockk(), mockk())

            shouldThrow<BadCredentialsException> {
                useCase.refresh("invalid-token")
            }
        }

        it("만료된 토큰이면 BadCredentialsException 을 던진다") {
            val expiredToken = RefreshToken(
                userId = 1L,
                token = "expired",
                expiresAt = Instant.now().minusSeconds(3600),   // 1시간 전 만료
            )
            val refreshTokenLoadPort = mockk<RefreshTokenLoadPort>()
            every { refreshTokenLoadPort.findByTokenValue(any()) } returns expiredToken
            val useCase = RefreshUseCase(refreshTokenLoadPort, mockk(), mockk(), mockk())

            shouldThrow<BadCredentialsException> {
                useCase.refresh("expired")
            }
        }

        it("유저가 없으면 BadCredentialsException 을 던진다") {
            val token = RefreshToken.create(userId = 999L)
            val refreshTokenLoadPort = mockk<RefreshTokenLoadPort>()
            val userLoadPort = mockk<UserLoadPort>()
            every { refreshTokenLoadPort.findByTokenValue(any()) } returns token
            every { userLoadPort.findById(999L) } returns null
            val useCase = RefreshUseCase(refreshTokenLoadPort, userLoadPort, mockk(), mockk())

            shouldThrow<BadCredentialsException> {
                useCase.refresh(token.token)
            }
        }

        it("비활성 유저면 BadCredentialsException 을 던진다") {
            val token = RefreshToken.create(userId = 1L)
            val inactiveUser = User(
                id = 1L,
                email = "a@b.com",
                password = "p",
                name = "n",
                isActive = false,
                userType = UserType.ADMIN,
            )
            val refreshTokenLoadPort = mockk<RefreshTokenLoadPort>()
            val userLoadPort = mockk<UserLoadPort>()
            every { refreshTokenLoadPort.findByTokenValue(any()) } returns token
            every { userLoadPort.findById(1L) } returns inactiveUser
            val useCase = RefreshUseCase(refreshTokenLoadPort, userLoadPort, mockk(), mockk())

            shouldThrow<BadCredentialsException> {
                useCase.refresh(token.token)
            }
        }
    }
})
