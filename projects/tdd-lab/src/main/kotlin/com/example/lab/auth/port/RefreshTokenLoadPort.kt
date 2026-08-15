package com.example.lab.auth.port

import com.example.lab.domain.auth.RefreshToken

interface RefreshTokenLoadPort {
    fun findByTokenValue(token: String): RefreshToken?
}
