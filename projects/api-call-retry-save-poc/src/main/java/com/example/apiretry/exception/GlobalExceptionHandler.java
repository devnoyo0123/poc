package com.example.apiretry.exception;

import com.example.apiretry.dto.ApiCallResponse;
import com.example.apiretry.service.SlackNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final SlackNotificationService slackNotificationService;

    public GlobalExceptionHandler(SlackNotificationService slackNotificationService) {
        this.slackNotificationService = slackNotificationService;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiCallResponse> handleApiException(ApiException e) {
        log.error("API Exception occurred: URL={}, Attempt={}", e.getUrl(), e.getAttemptCount());

        // 최종 실패 알림
        slackNotificationService.notifyFinalFailure(e.getUrl(), e.getAttemptCount(), e.getMessage());

        // 실패 응답 반환
        ApiCallResponse response = ApiCallResponse.builder()
                .endpoint(e.getUrl())
                .status("FAILED")
                .errorMessage(e.getMessage())
                .attemptCount(e.getAttemptCount())
                .isSuccess(false)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * ExternalApiException 핸들러
     * 모든 외부 API (ESM, NAVER, KAKAO 등) 연동 실패 처리
     */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiCallResponse> handleExternalApiException(ExternalApiException e) {
        // 로그 레벨 구분 (재시도 실패는 ERROR, 즉시 실패는 WARN)
        if (e.isRetryExhausted()) {
            log.error("[{}] External API retry exhausted: URL={}, Attempts={}, ErrorCode={}",
                    e.getServiceName(), e.getUrl(), e.getAttemptCount(), e.getErrorCode());
        } else {
            log.warn("[{}] External API call failed: URL={}, ErrorCode={}",
                    e.getServiceName(), e.getUrl(), e.getErrorCode());
        }

        // Slack 알림 전송
        slackNotificationService.notifyExternalApiFailure(
                e.getServiceName(),
                e.getUrl(),
                e.getAttemptCount(),
                e.isRetryExhausted(),
                e.getErrorCode()
        );

        // 실패 응답 반환
        ApiCallResponse response = ApiCallResponse.builder()
                .endpoint(e.getUrl())
                .status("FAILED")
                .errorMessage(e.getMessage())
                .attemptCount(e.getAttemptCount())
                .isSuccess(false)
                .build();

        // HTTP 상태 코드 결정 (재시도 실패: 503, 즉시 실패: 에러 코드 기반)
        HttpStatus httpStatus = e.isRetryExhausted()
                ? HttpStatus.SERVICE_UNAVAILABLE  // 503
                : determineHttpStatus(e.getErrorCode());

        return ResponseEntity.status(httpStatus).body(response);
    }

    /**
     * 에러 코드 기반 HTTP 상태 코드 결정
     */
    private HttpStatus determineHttpStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        try {
            int statusCode = Integer.parseInt(errorCode);
            return HttpStatus.valueOf(statusCode);
        } catch (Exception e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
