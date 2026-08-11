package com.example.lab.auth.usecase

import com.example.lab.auth.dto.LoginRequest
import com.example.lab.auth.dto.LoginResult
import com.example.lab.auth.port.AccessTokenIssuer
import com.example.lab.auth.port.RefreshTokenIssuer
import com.example.lab.auth.port.UserLoadPort
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder

class LoginUseCase(
    private val userLoadPort: UserLoadPort,
    private val accessTokenIssuer: AccessTokenIssuer,
    private val refreshTokenIssuer: RefreshTokenIssuer,
    private val passwordEncoder: PasswordEncoder,
) {
    fun login(request: LoginRequest): LoginResult {
        val user = userLoadPort.findByEmail(request.email)
            ?: throw BadCredentialsException("Invalid credentials")

        if(!user.canLogin()) {
            throw BadCredentialsException("User is not active")
        }

        if(!user.matchesPassword(request.password, passwordEncoder)) {
            throw BadCredentialsException("Invalid password")
        }

        val access = accessTokenIssuer.issue(user)
        val userId = requireNotNull(user.id) { "user id must be persisted" }
        val refreshTokenValue = refreshTokenIssuer.issue(userId)

        return LoginResult(
            accessToken = access.token,
            tokenType = TOKEN_TYPE,
            expiresIn = access.expiresIn,
            userInfo = LoginResult.UserInfo(
                userId = userId.toString(),
                userType = user.userType.name,
                name = user.name,
                email = user.email,
            ),
            refreshTokenValue = refreshTokenValue,
        )
    }

    companion object {
        private const val TOKEN_TYPE = "Bearer"
    }
}
