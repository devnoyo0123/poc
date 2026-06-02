package com.example.concurrency.service

import com.example.concurrency.entity.UserAccount
import com.example.concurrency.repository.UserAccountRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap

@Service
class UserAccountService(
    private val repository: UserAccountRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 동시성 테스트를 위한 상태 추적
    private val creationAttempts = ConcurrentHashMap<String, Int>()
    private val creationSuccesses = ConcurrentHashMap<String, Int>()
    private val creationFailures = ConcurrentHashMap<String, Int>()

    /**
     * Strategy 1: 단순 저장 - unique 제약조건 위주
     */
    @Transactional
    fun createUserAccountSimple(
        email: String,
        username: String,
        fullName: String
    ): UserAccount {
        val attemptKey = email
        creationAttempts.compute(attemptKey) { _, v -> (v ?: 0) + 1 }

        return try {
            val user = UserAccount(
                email = email,
                username = username,
                fullName = fullName
            )
            val saved = repository.save(user)
            creationSuccesses.compute(attemptKey) { _, v -> (v ?: 0) + 1 }
            log.info("✅ 생성 성공: $email")
            saved
        } catch (e: DataIntegrityViolationException) {
            creationFailures.compute(attemptKey) { _, v -> (v ?: 0) + 1 }
            log.warn("❌ 생성 실패 (unique 위반): $email - ${e.message}")
            throw e
        }
    }

    /**
     * Strategy 2: SELECT 후 저장 (Race Condition 가능성 있음)
     */
    @Transactional
    fun createUserAccountWithCheck(
        email: String,
        username: String,
        fullName: String
    ): UserAccount? {
        val existing = repository.findByEmail(email)
        if (existing != null) {
            log.warn("이미 존재하는 이메일: $email")
            return null
        }

        return try {
            val user = UserAccount(
                email = email,
                username = username,
                fullName = fullName
            )
            repository.save(user)
        } catch (e: DataIntegrityViolationException) {
            log.warn("Race Condition 발생: $email")
            throw e
        }
    }

    /**
     * Strategy 3: Native Lock with SELECT FOR UPDATE
     */
    @Transactional
    fun createUserAccountWithLock(
        email: String,
        username: String,
        fullName: String
    ): UserAccount {
        // pessimistic lock으로 row 잠금
        val existing = repository.findByEmail(email)
        if (existing != null) {
            log.warn("이미 존재하는 이메일 (lock): $email")
            return existing
        }

        return try {
            val user = UserAccount(
                email = email,
                username = username,
                fullName = fullName
            )
            repository.save(user)
        } catch (e: DataIntegrityViolationException) {
            log.warn("Lock 후에도 충돌 발생: $email")
            throw e
        }
    }

    fun findByEmail(email: String): UserAccount? {
        return repository.findByEmail(email)
    }

    fun count(): Long {
        return repository.count()
    }

    // 테스트 통계
    fun getStats(email: String): Map<String, Int> {
        return mapOf(
            "attempts" to (creationAttempts[email] ?: 0),
            "successes" to (creationSuccesses[email] ?: 0),
            "failures" to (creationFailures[email] ?: 0)
        )
    }

    fun clearStats() {
        creationAttempts.clear()
        creationSuccesses.clear()
        creationFailures.clear()
    }
}
