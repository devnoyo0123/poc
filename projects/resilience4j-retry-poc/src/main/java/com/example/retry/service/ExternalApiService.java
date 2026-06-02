package com.example.retry.service;

import com.example.retry.dto.ApiResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 외부 API 호출 시 Retry 동작을 테스트하는 Service
 * 400, 404 같은 클라이언트 에러는 retry 하지 않음
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalApiService {

    private final RestTemplate restTemplate;
    private final AtomicInteger callCount = new AtomicInteger(0);

    /**
     * Retry 설정이 적용된 API 호출 (400, 404는 제외)
     */
    @Retry(name = "externalApiService", fallbackMethod = "fallbackForExternalApi")
    public ApiResponse callExternalApi(String endpoint) {
        int currentCount = callCount.incrementAndGet();
        log.info("=== External API Call #{} - endpoint: {}", currentCount, endpoint);

        String url = "http://localhost:8080/api/demo/" + endpoint;
        ApiResponse response = restTemplate.getForObject(url, ApiResponse.class);

        log.info("=== External API Call #{} - SUCCESS", currentCount);
        return response;
    }

    /**
     * Fallback 메서드 - 모든 retry 실패 시 호출
     */
    public ApiResponse fallbackForExternalApi(String endpoint, Exception e) {
        log.warn("=== All retries failed for endpoint: {} - Error: {}", endpoint, e.getMessage());

        if (e instanceof HttpClientErrorException) {
            HttpClientErrorException httpEx = (HttpClientErrorException) e;
            log.warn("=== Client Error - Status: {}, No retry performed", httpEx.getStatusCode());
        }

        return ApiResponse.builder()
                .status("error")
                .message("Fallback: " + e.getMessage())
                .callCount(callCount.get())
                .build();
    }

    public void resetCounter() {
        callCount.set(0);
        log.info("External API call counter reset to 0");
    }

    public int getCallCount() {
        return callCount.get();
    }
}
