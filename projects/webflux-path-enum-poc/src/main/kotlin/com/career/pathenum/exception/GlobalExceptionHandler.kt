package com.career.pathenum.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

/**
 * 에러 응답 바디. ISO-8601 timestamp 와 exception 타입명을 포함해 디버깅을 돕는다.
 */
data class ErrorResponse(
    val timestamp: String,
    val status: Int,
    val error: String,
    val message: String,
    val exception: String
)

/**
 * 글로벌 예외 핸들러.
 * 도메인 예외 타입 → HTTP 상태 코드 매핑을 한 곳에서 처리한다.
 *  - NotFound 계열 → 404
 *  - 도메인 규칙 위반(PostMismatch, InvalidDepth) → 400
 *  - 미처리 예외 → 500 (로그 남김)
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(CommentNotFoundException::class)
    fun handleCommentNotFound(ex: CommentNotFoundException): ResponseEntity<ErrorResponse> {
        return build(HttpStatus.NOT_FOUND, ex)
    }

    @ExceptionHandler(ParentNotFoundException::class)
    fun handleParentNotFound(ex: ParentNotFoundException): ResponseEntity<ErrorResponse> {
        return build(HttpStatus.NOT_FOUND, ex)
    }

    @ExceptionHandler(PostMismatchException::class)
    fun handlePostMismatch(ex: PostMismatchException): ResponseEntity<ErrorResponse> {
        return build(HttpStatus.BAD_REQUEST, ex)
    }

    @ExceptionHandler(InvalidDepthException::class)
    fun handleInvalidDepth(ex: InvalidDepthException): ResponseEntity<ErrorResponse> {
        return build(HttpStatus.BAD_REQUEST, ex)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return build(HttpStatus.BAD_REQUEST, ex)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("unexpected error", ex)
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex)
    }

    private fun build(
        status: HttpStatus,
        ex: Throwable
    ): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            timestamp = LocalDateTime.now().toString(),
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message ?: status.reasonPhrase,
            exception = ex.javaClass.simpleName
        )
        return ResponseEntity.status(status).body(body)
    }
}
