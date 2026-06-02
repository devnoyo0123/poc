package com.example.payment.retry

import com.example.payment.exceptions.RetryExhaustedException
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * 재시도 정책
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000,  // 초기 지연 (1초)
    val maxDelayMs: Long = 10000,      // 최대 지연 (10초)
    val multiplier: Double = 2.0        // 지연 증가 배수
) {
    /**
     * 지수 백오프로 지연 시간 계산
     * 1s, 2s, 4s, 8s, ...
     */
    fun getDelayMs(attempt: Int): Long {
        if (attempt <= 0) return 0
        val delay = (initialDelayMs * multiplier.pow(attempt - 1)).toLong()
        return min(delay, maxDelayMs)
    }

    /**
     * 재시도 가능한 예외인지 확인
     */
    fun shouldRetry(exception: Throwable): Boolean {
        return when (exception) {
            is java.net.SocketTimeoutException,
            is java.net.UnknownHostException,
            is kotlinx.coroutines.TimeoutCancellationException -> true
            else -> false
        }
    }

    /**
     * 재시도 로직 실행
     */
    suspend fun <T> execute(block: suspend () -> T): T {
        var lastException: Throwable? = null

        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e

                // 재시료 불가능한 예외면 즉시 실패
                if (!shouldRetry(e)) {
                    throw e
                }

                // 마지막 시도면 예외 던짐
                if (attempt == maxAttempts) {
                    break
                }

                // 지연 후 재시도
                val delayMs = getDelayMs(attempt)
                println("재시도 $attempt/${maxAttempts} after ${delayMs}ms - ${e.message}")
                delay(delayMs)
            }
        }

        throw RetryExhaustedException(
            "최대 ${maxAttempts}회 재시도 실패",
            lastException ?: Exception("알 수 없는 오류")
        )
    }
}
