package com.example.lab.auth.port

import com.example.lab.domain.auth.User

interface UserLoadPort {
    fun findByEmail(email: String): User?
}

interface AccessTokenIssuer {
    fun issue(user: User): TokenIssue

    data class TokenIssue(
        val token: String,
        val expiresIn: Int,
    )
}

interface RefreshTokenIssuer {
    fun issue(userId: Long): String
}
