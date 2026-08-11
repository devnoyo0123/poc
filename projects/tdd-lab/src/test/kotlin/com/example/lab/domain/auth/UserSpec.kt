package com.example.lab.domain.auth

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin
import com.navercorp.fixturemonkey.kotlin.giveMeBuilder
import com.navercorp.fixturemonkey.kotlin.set
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.security.crypto.password.PasswordEncoder

class UserSpec() : DescribeSpec({

    // NoOp 인코더 — 인코딩 없이 그대로 반환하는 결정적 테스트 더블.
    // matchesPassword 자체(위임이 올바른가)만 고립 검증. 진짜 암호화 작동은 통합 테스트로.
    fun noOpEncoder(): PasswordEncoder = object : PasswordEncoder {
        override fun encode(raw: CharSequence): String = raw.toString()
        override fun matches(raw: CharSequence, encoded: String): Boolean = raw.toString() == encoded
    }

    val fixtureMonkey = FixtureMonkey.builder()
        .plugin(KotlinPlugin())
        .build()

    describe("User - 비밀번호 매칭 (PasswordEncoder)") {
        it("createForAdmin 으로 만든 유저는 올바른 raw 비밀번호로 검증된다") {
            val user = User.createForAdmin(
                email = "admin@example.com",
                rawPassword = "password123!",
                name = "관리자",
                passwordEncoder = noOpEncoder(),
            )

            user.matchesPassword("password123!", noOpEncoder()) shouldBe true
        }

        it("틀린 raw 비밀번호는 검증에 실패한다") {
            val user = User.createForAdmin(
                email = "admin@example.com",
                rawPassword = "password123!",
                name = "관리자",
                passwordEncoder = noOpEncoder(),
            )

            user.matchesPassword("WRONG", noOpEncoder()) shouldBe false
        }
    }

    describe("User - 로그인 가능 여부") {
        it("활성 + 이메일 인증된 유저는 로그인할 수 있다") {
            val user = User(
                email = "admin@example.com",
                password = "encoded:password123!",
                isActive = true,
                isEmailVerified = true,
                userType = UserType.WRITER
            )

            user.canLogin() shouldBe true
        }

        it("비활성 유저는 로그인할 수 없다") {
            val user = User(
                email = "admin@example.com",
                password = "encoded:password123!",
                isActive = false,
                isEmailVerified = true,
                userType = UserType.WRITER
            )

            user.canLogin() shouldBe false
        }

        it("이메일 미인증 유저도 로그인할 수 있다") {
            val user = User(
                email = "admin@example.com",
                password = "encoded:password123!",
                isActive = true,
                isEmailVerified = false,
                userType = UserType.WRITER
            )

            user.canLogin() shouldBe true
        }
    }

    describe("User - 관리자/작성자 구분") {
        it("ADMIN 타입이면 idAdmin 이 true 다") {
            val user: User = fixtureMonkey.giveMeBuilder<User>()
                .set(User::userType, UserType.ADMIN)
                .sample()

            user.isAdmin() shouldBe true
        }
    }

})
