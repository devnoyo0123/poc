package com.example.retry.controller;

import com.example.retry.dto.ApiResponse;
import com.example.retry.entity.SampleEntity;
import com.example.retry.service.RetryableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/retry")
@RequiredArgsConstructor
@Slf4j
public class RetryController {

    private final RetryableService retryableService;
    private final RestTemplate restTemplate;
    private final com.example.retry.service.ExternalApiService externalApiService;

    /**
     * Retry 성공 테스트
     * - 1차 시도: 실패 (retry)
     * - 2차 시도: 성공 (커밋)
     */
    @PostMapping("/success")
    public ResponseEntity<Map<String, Object>> testRetrySuccess(@RequestParam(defaultValue = "test-item") String name) {
        log.info("=== API Called: /api/retry/success with name={}", name);

        retryableService.resetCounter();

        try {
            SampleEntity result = retryableService.saveWithRetry(name, 2); // 2번째 시도에 성공

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Retry succeeded");
            response.put("entity", result);
            response.put("attemptCount", retryableService.getCurrentAttemptCount());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * MaxAttempts 초과 테스트
     * - 1~5차 시도: 모두 실패
     * - 최종적으로 예외 발생
     */
    @PostMapping("/max-attempts")
    public ResponseEntity<Map<String, Object>> testMaxAttemptsExceeded(@RequestParam(defaultValue = "test-item") String name) {
        log.info("=== API Called: /api/retry/max-attempts with name={}", name);

        retryableService.resetCounter();

        try {
            retryableService.saveWithMaxRetry(name);

            // 여기까지 오면 안됨
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Should not reach here - max attempts should have thrown exception");
            return ResponseEntity.internalServerError().body(response);

        } catch (Exception e) {
            log.info("Expected exception after max attempts: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Max attempts exceeded as expected");
            response.put("attemptCount", retryableService.getCurrentAttemptCount());
            response.put("exceptionMessage", e.getMessage());

            return ResponseEntity.ok(response);
        }
    }

    /**
     * Retry 중 롤백 후 최종 커밋 테스트
     * - 1차 시도: DB 저장 후 실패 (롤백 + retry)
     * - 2차 시도: DB 저장 후 실패 (롤백 + retry)
     * - 3차 시도: 성공 (커밋)
     */
    @PostMapping("/rollback-and-commit")
    public ResponseEntity<Map<String, Object>> testRollbackAndCommit(@RequestParam(defaultValue = "test-item") String name) {
        log.info("=== API Called: /api/retry/rollback-and-commit with name={}", name);

        retryableService.resetCounter();

        try {
            SampleEntity result = retryableService.saveWithRollbackAndRetry(name, 3); // 3번째 시도에 성공

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Retry with rollback succeeded");
            response.put("entity", result);
            response.put("attemptCount", retryableService.getCurrentAttemptCount());
            response.put("note", "Previous attempts were rolled back, only final attempt was committed");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Unexpected error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 카운터 리셋
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetCounter() {
        retryableService.resetCounter();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Counter reset successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Demo API 호출 테스트 - 400 Bad Request (retry 하지 않음)
     */
    @GetMapping("/test/400")
    public ResponseEntity<Map<String, Object>> test400BadRequest() {
        log.info("=== Testing 400 Bad Request - should NOT retry");
        // ExternalApiService 사용 (여기에 @Retry 적용됨)
        ApiResponse response = externalApiService.callExternalApi("bad-request");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("response", response);
        result.put("callCount", externalApiService.getCallCount());
        result.put("note", "400 error should not retry - callCount should be 1");
        return ResponseEntity.ok(result);
    }

    /**
     * Demo API 호출 테스트 - 404 Not Found (retry 하지 않음)
     */
    @GetMapping("/test/404")
    public ResponseEntity<Map<String, Object>> test404NotFound() {
        log.info("=== Testing 404 Not Found - should NOT retry");
        // ExternalApiService 사용 (여기에 @Retry 적용됨)
        ApiResponse response = externalApiService.callExternalApi("not-found?id=test123");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("response", response);
        result.put("callCount", externalApiService.getCallCount());
        result.put("note", "404 error should not retry - callCount should be 1");
        return ResponseEntity.ok(result);
    }

    /**
     * Demo API 호출 테스트 - 500 Server Error (retry 수행)
     */
    @GetMapping("/test/500")
    public ResponseEntity<Map<String, Object>> test500ServerError() {
        log.info("=== Testing 500 Server Error - SHOULD retry 3 times");
        // ExternalApiService 사용 (여기에 @Retry 적용됨)
        ApiResponse response = externalApiService.callExternalApi("server-error");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("response", response);
        result.put("callCount", externalApiService.getCallCount());
        result.put("note", "500 error should retry 3 times - callCount should be 3");
        return ResponseEntity.ok(result);
    }

    /**
     * Demo API 호출 테스트 - 성공 (retry 안함)
     */
    @GetMapping("/test/success")
    public ResponseEntity<Map<String, Object>> testSuccess() {
        log.info("=== Testing 200 Success - should NOT retry");
        // ExternalApiService 사용 (여기에 @Retry 적용됨)
        ApiResponse response = externalApiService.callExternalApi("success");
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("response", response);
        result.put("callCount", externalApiService.getCallCount());
        return ResponseEntity.ok(result);
    }

    /**
     * 외부 API 서비스 리셋
     */
    @PostMapping("/test/reset-api-count")
    public ResponseEntity<Map<String, String>> resetApiCounter() {
        externalApiService.resetCounter();
        Map<String, String> response = new HashMap<>();
        response.put("message", "External API call counter reset successfully");
        return ResponseEntity.ok(response);
    }
}
