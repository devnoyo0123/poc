package com.example.concurrency.repository

import com.example.concurrency.entity.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserAccountRepository : JpaRepository<UserAccount, Long> {
    fun findByEmail(email: String): UserAccount?
    fun findByUsername(username: String): UserAccount?
}
