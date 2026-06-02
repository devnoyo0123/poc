package com.example.apiretry.service;

import com.example.apiretry.dto.ApiCallResponse;
import com.example.apiretry.entity.ApiCallAttempt;
import com.example.apiretry.entity.ApiCallResult;
import com.example.apiretry.exception.ApiException;
import com.example.apiretry.repository.ApiCallAttemptRepository;
import com.example.apiretry.repository.ApiCallResultRepository;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApiCallService {

    private final RestTemplate restTemplate;
    private final ApiCallResultRepository repository;
    private final ApiCallAttemptRepository attemptRepository;
    private final SlackNotificationService slackNotificationService;
    private final RetryRegistry retryRegistry;

    private static ApplicationContext applicationContext;

    // ThreadLocal to track retry count across retries
    private static ThreadLocal<Integer> currentAttemptCount = ThreadLocal.withInitial(() -> 1);

    @Autowired
    public ApiCallService(RestTemplate restTemplate,
                        ApiCallResultRepository repository,
                        ApiCallAttemptRepository attemptRepository,
                        SlackNotificationService slackNotificationService,
                        RetryRegistry retryRegistry,
                        ApplicationContext applicationContext) {
        this.restTemplate = restTemplate;
        this.repository = repository;
        this.attemptRepository = attemptRepository;
        this.slackNotificationService = slackNotificationService;
        this.retryRegistry = retryRegistry;
        ApiCallService.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        log.info("ApiCallService initialized with ApplicationContext");
    }

    /**
     * 외부 API를 호출하고 결과를 저장합니다.
     * 성공/실패와 상관없이 항상 결과를 저장합니다.
     *
     * @param url 호출할 API URL
     * @return 저장된 API 호출 결과
     */
    public ApiCallResponse callAndSaveApi(String url) {
        return callAndSaveApiWithRetry(url);
    }

    /**
     * 재시도 로직이 포함된 내부 메서드
     */
    @Retry(name = "apiCallRetry", fallbackMethod = "apiCallFallback")
    private ApiCallResponse callAndSaveApiWithRetry(String url) {
        log.info("Calling API: {}", url);

        try {
            // 현재 시도 횟수 가져오기 (ThreadLocal에서)
            int attemptCount = currentAttemptCount.get();
            log.info("Attempt #{}", attemptCount);

            // 외부 API 호출
            String responseBody = restTemplate.getForObject(url, String.class);

            // 이번 시도 저장 (성공)
            ApiCallAttempt attempt = ApiCallAttempt.builder()
                    .attemptNumber(attemptCount)
                    .statusCode(HttpStatus.OK.value())
                    .errorMessage(null)
                    .attemptTime(LocalDateTime.now())
                    .build();
            attemptRepository.save(attempt);
            log.info("Attempt #{} saved to attempts table", attemptCount);

            // 최종 성공 결과 저장
            ApiCallResult result = ApiCallResult.builder()
                    .endpoint(url)
                    .statusCode(HttpStatus.OK.value())
                    .status("SUCCESS")
                    .responseBody(responseBody != null ? responseBody.substring(0, Math.min(2000, responseBody.length())) : "No response body")
                    .attemptCount(attemptCount)
                    .isSuccess(true)
                    .callTime(LocalDateTime.now())
                    .build();

            ApiCallResult saved = repository.save(result);

            // 시도에 결과 ID 연결
            attempt.setResult(saved);
            attemptRepository.save(attempt);

            log.info("API call successful. Saved result ID: {}, Attempts: {}", saved.getId(), attemptCount);

            // ThreadLocal 정리
            currentAttemptCount.remove();

//            slackNotificationService.notifySuccess(url, attemptCount);
            return toResponse(saved);

        } catch (HttpServerErrorException | HttpClientErrorException.TooManyRequests e) {
            // 5xx 에러 또는 429 Too Many Requests - 재시도 대상
            log.error("Retryable error occurred: {} - {}", e.getStatusCode(), e.getMessage());
            int attemptCount = currentAttemptCount.get();

            // 이번 실패 시도 저장
            int statusCode = (e.getStatusCode() != null) ? e.getStatusCode().value() : 0;
            saveAttemptRecord(url, attemptCount, statusCode, e.getMessage());

            // 각 재시도 실패 시 Slack 알림
            slackNotificationService.notifyRetryFailure(url, attemptCount, e.getStatusCode().toString(), e.getMessage());

            // 다음 시도를 위해 카운트 증가
            currentAttemptCount.set(attemptCount + 1);
            throw e; // Retry가 이 예외를 catch하여 재시도

        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound |
                 HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden |
                 HttpClientErrorException.MethodNotAllowed | HttpClientErrorException.Conflict |
                 HttpClientErrorException.UnsupportedMediaType | HttpClientErrorException.UnprocessableEntity e) {
            // 4xx 에러 - 즉시 실패 (재시도 안 함)
            log.error("Immediate failure (4xx): {} - {}", e.getStatusCode(), e.getMessage());
            int attemptCount = currentAttemptCount.get();
            currentAttemptCount.remove();

            // 즉시 실패는 시도 기록 남기지 않음 (attempt 테이블에 남지 않음)
            // DB에 실패 결과 저장
            saveFailureResultToDb(url, attemptCount, e.getMessage());

            // ApiException 던지기 (alreadySaved=true) - GlobalExceptionHandler에서 Slack 알림 전송
            // fallback에서는 저장하지 않음
            throw new ApiException(e.getMessage(), url, attemptCount, true);
        } catch (Exception e) {
            // 기타 에러 - 재시도하지 않고 바로 실패
            log.error("Unexpected error occurred: {}", e.getMessage());
            int attemptCount = currentAttemptCount.get();
            currentAttemptCount.remove();

            // 기타 에러도 시도 기록 남기지 않음
            // DB에 실패 결과 저장
            saveFailureResultToDb(url, attemptCount, e.getMessage());

            // ApiException 던지기 (alreadySaved=true) - GlobalExceptionHandler에서 Slack 알림 전송
            // fallback에서는 저장하지 않음
            throw new ApiException(e.getMessage(), url, attemptCount, true);
        }
    }

    /**
     * 실패 결과를 DB에 저장하는 헬퍼 메서드
     * apiCallFallback과 즉시 실패 catch 블록에서 모두 사용합니다.
     */
    private void saveFailureResultToDb(String url, int attemptCount, String errorMessage) {
        try {
            ApiCallResultRepository resultRepo = applicationContext.getBean(ApiCallResultRepository.class);
            ApiCallResult result = ApiCallResult.builder()
                    .endpoint(url)
                    .statusCode(0)
                    .status("FAILED")
                    .errorMessage(errorMessage)
                    .attemptCount(attemptCount)
                    .isSuccess(false)
                    .callTime(LocalDateTime.now())
                    .build();

            ApiCallResult saved = resultRepo.save(result);
            log.info("Failure result saved. ID: {}, Attempts: {}", saved.getId(), attemptCount);
        } catch (Exception dbError) {
            log.error("Failed to save failure result: {}", dbError.getMessage());
            // DB 저장 실패 시도 RuntimeException 던지기
            throw new RuntimeException("Failed to save API call result", dbError);
        }
    }

    /**
     * Retry limit 초과 시 Fallback 메서드
     * 최종 실패 결과를 DB에 저장한 후 ApiException을 던집니다.
     * @ExceptionHandler에서 Slack 알림을 전송합니다.
     */
    private ApiCallResponse apiCallFallback(String url, Exception e) {
        int attemptCount = currentAttemptCount.get();
        log.error("Retry limit exceeded for URL: {}. Saving final failure result. Attempts: {}", url, attemptCount);

        // ThreadLocal 정리
        currentAttemptCount.remove();

        // 최종 실패 결과 저장
        saveFailureResultToDb(url, attemptCount, "Retry limit exceeded after " + attemptCount + " attempts");

        // ApiException 던지기 - GlobalExceptionHandler에서 Slack 알림 전송
        throw new ApiException("Retry limit exceeded after " + attemptCount + " attempts", url, attemptCount);
    }

    /**
     * 특정 엔드포인트의 호출 이력을 조회합니다.
     *
     * @param endpoint 조회할 엔드포인트 URL
     * @return 호출 이력 목록 (최신순)
     */
    public List<ApiCallResponse> getCallHistory(String endpoint) {
        log.info("Fetching call history for endpoint: {}", endpoint);
        List<ApiCallResult> results = repository.findByEndpointOrderByCallTimeDesc(endpoint);
        return results.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 각 시도 기록을 저장합니다.
     */
    private void saveAttemptRecord(String url, int attemptNumber, Integer statusCode, String errorMessage) {
        saveAttemptRecordWithRepo(url, attemptNumber, statusCode, errorMessage, attemptRepository);
    }

    /**
     * 각 시도 기록을 저장합니다 (Repository를 직접 받는 버전 - Fallback용)
     */
    private void saveAttemptRecordWithRepo(String url, int attemptNumber, Integer statusCode, String errorMessage, ApiCallAttemptRepository repo) {
        try {
            ApiCallAttempt attempt = ApiCallAttempt.builder()
                    .attemptNumber(attemptNumber)
                    .statusCode(statusCode)
                    .errorMessage(errorMessage != null && errorMessage.length() > 1000 ? errorMessage.substring(0, 1000) : errorMessage)
                    .attemptTime(LocalDateTime.now())
                    .build();
            repo.save(attempt);
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
}
