package com.example.payment.config

/**
 * SDK 설정 클래스
 *
 * @property apiKey 결제 서비스 API 키
 * @property baseUrl 결제 서비스 Base URL
 * @property timeoutSeconds API 호출 타임아웃 (초)
 * @property maxRetries 최대 재시도 횟수
 * @property enableLogging 로그 활성화 여부
 */
data class PaymentConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.payment.internal",
    val timeoutSeconds: Long = 30,
    val maxRetries: Int = 3,
    val enableLogging: Boolean = true
) {
    companion object {
        /**
         * 환경 변수로부터 설정 생성
         */
        fun fromEnv(): PaymentConfig {
            val apiKey = System.getenv("PAYMENT_API_KEY")
                ?: throw IllegalArgumentException("PAYMENT_API_KEY 환경 변수가 필요합니다")

            return PaymentConfig(
                apiKey = apiKey,
                baseUrl = System.getenv("PAYMENT_BASE_URL") ?: "https://api.payment.internal",
                timeoutSeconds = System.getenv("PAYMENT_TIMEOUT")?.toLongOrNull() ?: 30,
                maxRetries = System.getenv("PAYMENT_MAX_RETRIES")?.toIntOrNull() ?: 3,
                enableLogging = System.getenv("PAYMENT_ENABLE_LOGGING")?.toBoolean() ?: true
            )
        }

        /**
         * 테스트용 모의 설정
         */
        fun forTest(): PaymentConfig {
            return PaymentConfig(
                apiKey = "test-api-key",
                baseUrl = "http://localhost:8080",
                timeoutSeconds = 5,
                maxRetries = 1,
                enableLogging = false
            )
        }
    }
}
