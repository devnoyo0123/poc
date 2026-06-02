package com.example.payment.exceptions

/**
 * 결제 SDK 기본 예외
 */
sealed class PaymentException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * API 호출 실패
 */
class ApiException(
    val statusCode: Int,
    val errorCode: String? = null,
    message: String,
    cause: Throwable? = null
) : PaymentException(message, cause)

/**
 * 인증 실패
 */
class AuthenticationException(message: String) : PaymentException(message)

/**
 * 네트워크 오류
 */
class NetworkException(message: String, cause: Throwable? = null) : PaymentException(message, cause)

/**
 * 타임아웃
 */
class TimeoutException(message: String) : PaymentException(message)

/**
 * 재시도 실패 (최대 재시도 횟수 초과)
 */
class RetryExhaustedException(message: String, val lastError: Throwable) : PaymentException(message, lastError)

/**
 * 잘못된 요청
 */
class BadRequestException(
    val field: String? = null,
    message: String
) : PaymentException(message)
