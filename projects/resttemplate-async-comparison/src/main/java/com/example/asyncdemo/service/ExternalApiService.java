package com.example.asyncdemo.service;

import com.example.asyncdemo.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiService {

    private final RestTemplate restTemplate;

    @Value("${external-api.delay-ms:1000}")
    private long delayMs;

    /**
     * 사용자 정보 조회 API 호출 (순차)
     * 실제 외부 API처럼 지연 시간을 시뮬레이션
     */
    public UserResponse fetchUser(Long userId) {
        long startTime = System.currentTimeMillis();

        // 실제 외부 API 호출 대신 시뮬레이션
        // 실제 환경에서는: restTemplate.getForObject("http://external-api/users/" + userId, UserResponse.class);
        UserResponse response = simulateExternalApiCall(userId);

        long responseTime = System.currentTimeMillis() - startTime;
        response.setResponseTimeMs(responseTime);

        log.info("API 호출 완료 - userId: {}, responseTime: {}ms", userId, responseTime);
        return response;
    }

    /**
     * 비동기 사용자 정보 조회
     * Java 17에서는 Virtual Thread를 사용할 수 없으므로 일반 스레드 풀 사용
     */
    private static final Executor executor = Executors.newFixedThreadPool(10);

    public CompletableFuture<UserResponse> fetchUserAsync(Long userId) {
        return CompletableFuture.supplyAsync(() -> fetchUser(userId), executor);
    }

    /**
     * 외부 API 호출 시뮬레이션
     * 네트워크 지연을 흉내내기 위해 sleep
     */
    private UserResponse simulateExternalApiCall(Long userId) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("API 호출 중 인터럽트 발생", e);
        }

        return UserResponse.builder()
                .id(userId)
                .name("User" + userId)
                .email("user" + userId + "@example.com")
                .source("API-" + userId)
                .build();
    }
}
