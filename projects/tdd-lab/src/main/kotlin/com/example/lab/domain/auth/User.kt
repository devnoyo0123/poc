package com.example.lab.domain.auth

import org.springframework.security.crypto.password.PasswordEncoder

class User(
    val id: Long? = null,
    val email: String,
    val password: String,
    val name: String = "",
    val isActive: Boolean = true,
    val isEmailVerified: Boolean = false,
    val userType: UserType = UserType.WRITER,
) {
    fun matchesPassword(rawPassword: CharSequence, encoder: PasswordEncoder): Boolean =
        encoder.matches(rawPassword, password)

    fun canLogin(): Boolean = isActive

    fun isAdmin(): Boolean = userType == UserType.ADMIN

    companion object {
        fun createForAdmin(
            email: String,
            rawPassword: String,
            name: String,
            passwordEncoder: PasswordEncoder,
        ): User = User(
            email = email,
            password = passwordEncoder.encode(rawPassword),
            name = name,
            isActive = true,
            isEmailVerified = false,
            userType = UserType.ADMIN,
        )
    }
}
