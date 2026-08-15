package com.example.lab.auth.usecase

import com.example.lab.auth.port.AccessTokenIssuer
import com.example.lab.auth.port.RefreshTokenIssuer
import com.example.lab.auth.port.RefreshTokenLoadPort
import com.example.lab.auth.port.UserLoadPort
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service

@Service
class RefreshUseCase(
    private val refreshTokenLoadPort: RefreshTokenLoadPort,
    private val userLoadPort: UserLoadPort,
    private val accessTokenIssuer: AccessTokenIssuer,
    private val refreshTokenIssuer: RefreshTokenIssuer
) {
    fun refresh(token: String): RefreshResult {
        val refreshToken = refreshTokenLoadPort.findByTokenValue(token)
            ?: throw BadCredentialsException("Invalid refresh token")

        if(!refreshToken.isValid()) {
            throw BadCredentialsException("Invalid refresh token")
        }
        val user = userLoadPort.findById(refreshToken.userId)
            ?: throw BadCredentialsException("Invalid refresh token")

        if(!user.canLogin()) {
            throw BadCredentialsException("Invalid refresh token")
        }

        val access = accessTokenIssuer.issue(user)
        val newRefreshToken = refreshTokenIssuer.issue(refreshToken.userId)

        return RefreshResult(
            refreshTokenValue = newRefreshToken,
            tokenType = "Bearer",
            expiresIn = access.expiresIn,
            accessToken = access.token
        )
    }
}
