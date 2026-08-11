package com.example.lab.domain.auth

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant

class RefreshTokenSpec : DescribeSpec({

    describe("RefreshToken - 생성") {
        it("create 로 만든 토큰은 isValid 가 true 다") {
            val token = RefreshToken.create(userId = 1L)

            token.isValid() shouldBe true
        }

        it("create 로 만든 토큰의 userId 는 전달한 값이다") {
            val token = RefreshToken.create(userId = 42L)

            token.userId shouldBe 42L
        }

        it("create 로 만든 토큰의 token 문자열은 비어있지 않다") {
            val token = RefreshToken.create(userId = 1L)

            token.token shouldNotBe ""
        }
    }

    describe("RefreshToken - 폐기") {
        it("revoke 호출 후에는 isRevoked 가 true 다") {
            val token = RefreshToken.create(userId = 1L)

            token.revoke()

            token.isRevoked() shouldBe true
        }

        it("revoke 된 토큰은 isValid 가 false 다") {
            val token = RefreshToken.create(userId = 1L)

            token.revoke()

            token.isValid() shouldBe false
        }
    }

    describe("RefreshToken - 만료") {
        it("만료된 토큰은 isExpired 가 true 다") {
            // 과거 만료 토큰 — expiresAt 을 과거로 직접 설정 (결정적)
            val expiredToken = RefreshToken(
                userId = 1L,
                token = "expired",
                expiresAt = Instant.now().minusSeconds(60 * 60),   // 1시간 전
            )

            expiredToken.isExpired() shouldBe true
        }

        it("만료되지 않은 토큰(create)은 isExpired 가 false 다") {
            val token = RefreshToken.create(userId = 1L)   // 14일 뒤 만료

            token.isExpired() shouldBe false
        }

        it("만료된 토큰은 isValid 가 false 다") {
            val expiredToken = RefreshToken(
                userId = 1L,
                token = "expired",
                expiresAt = Instant.now().minusSeconds(60 * 60),
            )

            expiredToken.isValid() shouldBe false
        }
    }
})
