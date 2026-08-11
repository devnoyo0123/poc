package com.example.lab.domain.auth

import java.time.Instant
import java.util.UUID

class RefreshToken(
    val userId: Long,
    val token: String,
    var expiresAt: Instant,
    var revokedAt: Instant? = null,
) {

    companion object {
        const val expirationDays = 14
        fun create(userId: Long): RefreshToken {
            return RefreshToken(
                userId = userId,
                token = UUID.randomUUID().toString(),
                expiresAt = Instant.now().plusSeconds((60 * 60 * 24 * expirationDays).toLong()),
            )
        }
    }

    fun isValid(): Boolean {
        return !isExpired() && revokedAt == null
    }

    fun revoke() {
        revokedAt = Instant.now()
    }

    fun isRevoked(): Boolean {
        return revokedAt != null
    }

    fun isExpired(): Boolean {
        return Instant.now().isAfter(expiresAt)
    }
}
