package com.example.apiretry.service;

import com.example.apiretry.dto.ApiCallResponse;
import com.example.apiretry.entity.ApiCallAttempt;
import com.example.apiretry.entity.ApiCallResult;
import com.example.apiretry.exception.ApiException;
import com.example.apiretry.repository.ApiCallAttemptRepository;
import com.example.apiretry.repository.ApiCallResultRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@Slf4j
public class ApiCallRetryServiceV2 {

    private final RestTemplate restTemplate;
    private final ApiCallResultRepository repository;
    private final ApiCallAttemptRepository attemptRepository;
    private final SlackNotificationService slackNotificationService;

    public ApiCallRetryServiceV2(RestTemplate restTemplate,
                                   ApiCallResultRepository repository,
                                   ApiCallAttemptRepository attemptRepository,
                                   SlackNotificationService slackNotificationService) {
        this.restTemplate = restTemplate;
        this.repository = repository;
        this.attemptRepository = attemptRepository;
        this.slackNotificationService = slackNotificationService;
    }

    /**
     * 외부 API를 호출하고 결과를 저장합니다 (버전 2)
     *
     * @param url 호출할 API URL
     * @return 저장된 API 호출 결과
     */
    @Retry(name = "apiCallRetryV2", fallbackMethod = "handleFallback")
    public ApiCallResponse callAndSaveApi(String url) {
        return callApiInternal(url);
    }

    /**
     * 실제 API 호출 로직
     */
    private ApiCallResponse callApiInternal(String url) {
        log.info("Calling API (v2): {}", url);

        // 버전 2에서는 attemptCount를 0으로 하드코딩
        int attemptCount = 0;
        log.info("Attempt #{} (v2)", attemptCount);

        try {
            // 외부 API 호출
            String responseBody = restTemplate.getForObject(url, String.class);

            log.info("API call successful (v2). Response length: {}", responseBody != null ? responseBody.length() : 0);

            // 성공 결과 생성
            ApiCallResult result = ApiCallResult.builder()
                    .endpoint(url)
                    .statusCode(HttpStatus.OK.value())
                    .status("SUCCESS")
                    .responseBody(responseBody != null ? responseBody.substring(0, Math.min(2000, responseBody.length())) : "No response body")
                    .attemptCount(attemptCount)
                    .isSuccess(true)
                    .callTime(LocalDateTime.now())
                    .build();

            // 성공/실패 상관없이 DB에 저장
            ApiCallResult saved = repository.save(result);
            log.info("API call result saved (v2). ID: {}", saved.getId());

            return toResponse(saved);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // 4xx/5xx HTTP 에러
            int statusCode = e.getStatusCode().value();
            saveFailedResult(url, statusCode, e.getMessage(), attemptCount);

            // 5xx 및 429는 재시도를 위해 원본 예외 다시 던지기
            if (e instanceof HttpServerErrorException
                    || e instanceof HttpClientErrorException.TooManyRequests) {
                throw e;
            } else {
                // 그 외 4xx 에러는 즉시 실패
                throw new ApiException(e.getMessage(), url, attemptCount, true);
            }
        } catch (Exception e) {
            log.error("Unexpected error occurred (v2): {}", e.getMessage());
            saveFailedResult(url, 0, e.getMessage(), attemptCount);
            throw e;  // 원본 예외 그대로 → Resilience4j가 RetryConfig 기준으로 판단
        }
    }

    /**
     * Retry limit 초과 시 Fallback 메서드 (버전 2)
     * 최종 실패 결과를 DB에 저장한 후 Slack 알림을 전송합니다.
     */
    private ApiCallResponse handleFallback(String url, Exception e) {
        int attemptCount = 0;
        log.error("Retry limit exceeded for URL: {}. Saving final failure result. Attempts: {}", url, attemptCount);

        throw new ApiException("Retry limit exceeded after " + attemptCount + " attempts", url, attemptCount);
    }

    /**
     * 각 시도 기록을 저장합니다.
     */
    private void saveAttemptRecord(String url, int attemptNumber, Integer statusCode, String errorMessage) {
        try {
            ApiCallAttempt attempt = ApiCallAttempt.builder()
                    .attemptNumber(attemptNumber)
                    .statusCode(statusCode)
                    .errorMessage(errorMessage != null && errorMessage.length() > 1000 ? errorMessage.substring(0, 1000) : errorMessage)
                    .attemptTime(LocalDateTime.now())
                    .build();
            attemptRepository.save(attempt);
            log.info("Attempt #{} recorded: status={}, error={}", attemptNumber, statusCode, errorMessage);
        } catch (Exception e) {
            log.error("Failed to save attempt record: {}", e.getMessage());
        }
    }

    private ApiCallResponse toResponse(ApiCallResult result) {
        return ApiCallResponse.builder()
                .id(result.getId())
                .endpoint(result.getEndpoint())
                .statusCode(result.getStatusCode())
                .status(result.getStatus())
                .responseBody(result.getResponseBody())
                .errorMessage(result.getErrorMessage())
                .attemptCount(result.getAttemptCount())
                .isSuccess(result.getIsSuccess())
                .callTime(result.getCallTime())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getUpdatedAt())
                .attempts(null)
                .build();
    }

    private void saveFailedResult(String url, int statusCode, String errorMessage, int attemptCount) {
        log.error("HTTP error occurred (v2): {} - {}", statusCode, errorMessage);

        ApiCallResult result = ApiCallResult.builder()
                .endpoint(url)
                .statusCode(statusCode)
                .status("FAILED")
                .errorMessage(errorMessage)
                .attemptCount(attemptCount)
                .isSuccess(false)
                .callTime(LocalDateTime.now())
                .build();

        ApiCallResult saved = repository.save(result);
        log.info("API call result saved (v2). ID: {}", saved.getId());
    }
}
