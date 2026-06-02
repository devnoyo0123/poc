package com.example.concurrency

import com.example.concurrency.entity.UserAccount
import com.example.concurrency.repository.UserAccountRepository
import com.example.concurrency.service.UserAccountService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("JPA 동시성 테스트: Unique 제약조건")
class UserAccountConcurrencyTest : AbstractConcurrencyTest() {

    @Autowired
    private lateinit var service: UserAccountService

    @Autowired
    private lateinit var repository: UserAccountRepository

    private val log = LoggerFactory.getLogger(javaClass)

    @BeforeEach
    fun setUp() {
        service.clearStats()
    }

    @AfterEach
    fun tearDown() {
        // 모든 테스트 데이터 정리
        repository.deleteAll()
        service.clearStats()
    }

    @Test
    @DisplayName("단일 스레드: 정상 생성")
    fun testSingleCreation() {
        val user = service.createUserAccountSimple(
            email = "test@example.com",
            username = "testuser",
            fullName = "Test User"
        )

        // ID는 트랜잭션 커밋 후 생성됨
        log.info("단일 생성 완료: $user")

        // 커밋 후 확인
        val count = repository.count()
        log.info("DB 저장 수: $count")
        assert(count == 1L) { "DB에 1개만 저장되어야 함 (실제: $count)" }
    }

    @Test
    @DisplayName("동시 요청: 같은 email로 10개 생성 시도 -> 1개만 성공")
    fun testConcurrentCreationWithSameEmail() {
        val email = "concurrent@example.com"
        val attempts = 10
        val executorService = Executors.newFixedThreadPool(attempts)

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val futures = mutableListOf<CompletableFuture<Void>>()

        val startTime = System.currentTimeMillis()

        repeat(attempts) { i ->
            val future = CompletableFuture.runAsync({
                try {
                    log.info("시도 #${i + 1}: $email 생성 요청")
                    service.createUserAccountSimple(
                        email = email,
                        username = "user$i",
                        fullName = "User $i"
                    )
                    successCount.incrementAndGet()
                    log.info("✅ 시도 #${i + 1}: 성공")
                } catch (e: DataIntegrityViolationException) {
                    failureCount.incrementAndGet()
                    log.warn("❌ 시도 #${i + 1}: 실패 - ${e.mostSpecificCause.message}")
                } catch (e: Exception) {
                    log.error("💥 시도 #${i + 1}: 예외 발생", e)
                }
            }, executorService)

            futures.add(future)
        }

        // 모든 요청 완료 대기
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        executorService.shutdown()

        val duration = System.currentTimeMillis() - startTime

        // 결과 검증
        val stats = service.getStats(email)
        val finalCount = repository.count()

        log.info("=== 동시성 테스트 결과 ===")
        log.info("소요 시간: ${duration}ms")
        log.info("총 시도: $attempts")
        log.info("성공: $successCount")
        log.info("실패: $failureCount")
        log.info("DB 실제 저장 수: $finalCount")
        log.info("서비스 통계: $stats")

        // 검증: 정확히 1개만 저장되어야 함
        assert(finalCount == 1L) {
            "DB에 정확히 1개만 저장되어야 함 (실제: $finalCount)"
        }

        // 검증: 1개만 성공, 나머지는 실패
        assert(successCount.get() == 1) {
            "정확히 1개의 요청만 성공해야 함 (실제: ${successCount.get()})"
        }
        assert(failureCount.get() == attempts - 1) {
            "${attempts - 1}개의 요청이 실패해야 함 (실제: ${failureCount.get()})"
        }

        // 저장된 사용자 확인
        val savedUser = service.findByEmail(email)
        assert(savedUser != null) { "저장된 사용자를 찾을 수 있어야 함" }
        log.info("저장된 사용자: $savedUser")
    }

    @Test
    @DisplayName("동시 요청: 서로 다른 email로 10개 생성 -> 모두 성공")
    fun testConcurrentCreationWithDifferentEmails() {
        val attempts = 10
        val executorService = Executors.newFixedThreadPool(attempts)

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val futures = mutableListOf<CompletableFuture<Void>>()

        repeat(attempts) { i ->
            val future = CompletableFuture.runAsync({
                try {
                    service.createUserAccountSimple(
                        email = "user$i@example.com",
                        username = "user$i",
                        fullName = "User $i"
                    )
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                    log.warn("Failed to create user$i: ${e.message}")
                }
            }, executorService)

            futures.add(future)
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
        executorService.shutdown()

        log.info("=== 서로 다른 이메일 동시성 테스트 ===")
        log.info("성공: $successCount")
        log.info("실패: $failureCount")
        log.info("DB 저장 수: ${repository.count()}")

        // 모두 성공해야 함
        assert(successCount.get() == attempts) {
            "모든 요청이 성공해야 함 (성공: ${successCount.get()}/$attempts)"
        }
        assert(repository.count() == attempts.toLong()) {
            "DB에 $attempts 개가 저장되어야 함 (실제: ${repository.count()})"
        }
    }

    @Test
    @DisplayName("Race Condition 테스트: SELECT 후 INSERT")
    fun testRaceConditionWithCheck() {
        val email = "race@example.com"
        val attempts = 10
        val executorService = Executors.newFixedThreadPool(attempts)

        val successCount = AtomicInteger(0)
        val nullReturns = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val futures = mutableListOf<CompletableFuture<Void>>()

        repeat(attempts) { i ->
            val future = CompletableFuture.runAsync({
                try {
                    val result = service.createUserAccountWithCheck(
                        email = email,
                        username = "race$i",
                        fullName = "Race $i"
                    )
                    if (result != null) {
                        successCount.incrementAndGet()
                        log.info("✅ 시도 #${i + 1}: 생성 성공")
                    } else {
                        nullReturns.incrementAndGet()
                        log.info("⚠️ 시도 #${i + 1}: 이미 존재함 (null 반환)")
                    }
                } catch (e: DataIntegrityViolationException) {
                    failureCount.incrementAndGet()
                    log.warn("❌ 시도 #${i + 1}: Unique 위반 - Race Condition 발생!")
                }
            }, executorService)

            futures.add(future)
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
        executorService.shutdown()

        log.info("=== Race Condition 테스트 결과 ===")
        log.info("성공 (생성): $successCount")
        log.info("Null 반환 (이미 존재): $nullReturns")
        log.info("Unique 위반 (Race Condition): $failureCount")
        log.info("DB 저장 수: ${repository.count()}")

        // 1개만 저장되어야 함
        assert(repository.count() == 1L) {
            "DB에 1개만 저장되어야 함 (실제: ${repository.count()})"
        }

        // Race Condition이 발생할 수 있음 (SELECT 후 INSERT 사이에 다른 트랜잭션이 INSERT 가능)
        if (failureCount.get() > 0) {
            log.warn("⚠️ Race Condition 감지: SELECT-INSERT 방식으로도 충돌 발생 가능!")
        }
    }

    @Test
    @DisplayName("동시 요청: 50개 동시 요청 스트레스 테스트")
    fun testStressWith50ConcurrentRequests() {
        val email = "stress@example.com"
        val attempts = 50
        val executorService = Executors.newFixedThreadPool(attempts)

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val futures = mutableListOf<CompletableFuture<Void>>()

        val startTime = System.currentTimeMillis()

        repeat(attempts) { i ->
            val future = CompletableFuture.runAsync({
                try {
                    service.createUserAccountSimple(
                        email = email,
                        username = "stress$i",
                        fullName = "Stress $i"
                    )
                    successCount.incrementAndGet()
                } catch (e: DataIntegrityViolationException) {
                    failureCount.incrementAndGet()
                }
            }, executorService)

            futures.add(future)
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
        executorService.shutdown()

        val duration = System.currentTimeMillis() - startTime

        log.info("=== 스트레스 테스트 결과 ($attempts 개 동시 요청) ===")
        log.info("소요 시간: ${duration}ms")
        log.info("성공: $successCount")
        log.info("실패: $failureCount")
        log.info("DB 저장 수: ${repository.count()}")

        // 1개만 저장되어야 함
        assert(service.count() == 1L) {
            "고부하 상황에서도 1개만 저장되어야 함 (실제: ${service.count()})"
        }
        assert(successCount.get() == 1) {
            "1개의 요청만 성공해야 함 (실제: ${successCount.get()})"
        }
    }
}
