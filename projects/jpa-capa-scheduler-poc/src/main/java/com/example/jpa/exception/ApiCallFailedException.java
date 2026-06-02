package com.example.jpa.exception;

/**
 * API 호출 실패 예외
 * HTTP 200 응답이지만 business 로직상 실패한 경우 발생
 */
public class ApiCallFailedException extends RuntimeException {

    public ApiCallFailedException(String message) {
        super(message);
    }

    public ApiCallFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
