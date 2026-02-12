package com.example.queue.exception

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AlreadyInQueueException::class)
    fun handleAlreadyInQueue(ex: AlreadyInQueueException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn { "Already in queue: ${ex.message}" }
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse(HttpStatus.CONFLICT.value(), ex.message ?: "Already in queue"))
        )
    }

    @ExceptionHandler(QueueNotFoundException::class)
    fun handleQueueNotFound(ex: QueueNotFoundException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn { "Queue not found: ${ex.message}" }
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.message ?: "Queue not found"))
        )
    }

    @ExceptionHandler(QueueFullException::class)
    fun handleQueueFull(ex: QueueFullException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn { "Queue is full: ${ex.message}" }
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), ex.message ?: "Queue is full"))
        )
    }

    @ExceptionHandler(InvalidQueueStateException::class)
    fun handleInvalidQueueState(ex: InvalidQueueStateException): Mono<ResponseEntity<ErrorResponse>> {
        logger.warn { "Invalid queue state: ${ex.message}" }
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.message ?: "Invalid queue state"))
        )
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationException(ex: WebExchangeBindException): Mono<ResponseEntity<ErrorResponse>> {
        val errors = ex.bindingResult.fieldErrors
            .map { "${it.field}: ${it.defaultMessage}" }
            .joinToString(", ")
        logger.warn { "Validation error: $errors" }
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation failed: $errors"))
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): Mono<ResponseEntity<ErrorResponse>> {
        logger.error(ex) { "Unexpected error: ${ex.message}" }
        return Mono.just(
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error"))
        )
    }
}

data class ErrorResponse(
    val status: Int,
    val message: String
)
