package com.example.lab.auth.usecase

data class RefreshResult(
    val refreshTokenValue: String,
    val tokenType: String,
    val expiresIn: Int,
    val accessToken: String
)
