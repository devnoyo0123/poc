package com.example.concurrency.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_accounts",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_email", columnNames = ["email"]),
        UniqueConstraint(name = "uk_username", columnNames = ["username"])
    ]
)
class UserAccount(
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @field:Column(unique = true, nullable = false, length = 100)
    val email: String,

    @field:Column(unique = true, nullable = false, length = 50)
    val username: String,

    @field:Column(nullable = false)
    val fullName: String,

    @field:Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @field:Version
    var version: Long? = null
) {
    override fun toString(): String {
        return "UserAccount(id=$id, email='$email', username='$username', fullName='$fullName', version=$version)"
    }
}
