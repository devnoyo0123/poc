package com.example.lab.auth.dto

data class LoginRequest(
    val email: String,
    val password: String,
)

data class LoginResult(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val userInfo: UserInfo,
    val refreshTokenValue: String,
) {
    data class UserInfo(
        val userId: String,
        val userType: String,
        val name: String,
        val email: String,
    )
}
