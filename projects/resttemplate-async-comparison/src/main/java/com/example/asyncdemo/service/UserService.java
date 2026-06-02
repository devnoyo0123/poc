package com.example.asyncdemo.service;

import com.example.asyncdemo.dto.ComparisonResult;
import com.example.asyncdemo.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final ExternalApiService externalApiService;

    /**
     * 순차적으로 API 호출
     * N개의 API를 순서대로 호출하므로 총 시간 = N * 각 API 응답시간
     */
    public ComparisonResult fetchUsersSequential(List<Long> userIds) {
        long startTime = System.currentTimeMillis();
        List<UserResponse> results = new ArrayList<>();

        log.info("=== 순차 호출 시작 === API 개수: {}", userIds.size());

        for (Long userId : userIds) {
            try {
                UserResponse response = externalApiService.fetchUser(userId);
                results.add(response);
            } catch (Exception e) {
                log.error("순차 호출 실패 - userId: {}, error: {}", userId, e.getMessage());
                results.add(createFallbackResponse(userId, e));
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        return ComparisonResult.builder()
                .executionType("SEQUENTIAL")
                .totalTimeMs(totalTime)
                .results(results)
                .apiCallCount(userIds.size())
                .avgTimePerCallMs((double) totalTime / userIds.size())
                .build();
    }

    /**
     * CompletableFuture를 사용한 병렬 API 호출
     * N개의 API를 동시에 호출하므로 총 시간 ≈ 가장 느린 API 응답시간
     */
    public ComparisonResult fetchUsersParallel(List<Long> userIds) {
        long startTime = System.currentTimeMillis();

        log.info("=== 병렬 호출 시작 === API 개수: {}", userIds.size());

        // 모든 API 호출을 CompletableFuture로 시작 (에러 처리 포함)
        List<CompletableFuture<UserResponse>> futures = userIds.stream()
                .map(userId -> externalApiService.fetchUserAsync(userId)
                        .exceptionally(ex -> {
                            log.error("병렬 호출 실패 - userId: {}, error: {}", userId, ex.getMessage());
                            return createFallbackResponse(userId, ex);
                        }))
                .toList();

        // 모든 Future가 완료될 때까지 대기
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        // 결과 수집
        List<UserResponse> results = allFutures.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                .join(); 

        long totalTime = System.currentTimeMillis() - startTime;

        return ComparisonResult.builder()
                .executionType("PARALLEL")
                .totalTimeMs(totalTime)
                .results(results)
                .apiCallCount(userIds.size())
                .avgTimePerCallMs((double) totalTime / userIds.size())
                .build();
    }

    /**
     * 에러 발생 시 대체 응답 생성
     */
    private UserResponse createFallbackResponse(Long userId, Throwable ex) {
        return UserResponse.builder()
                .id(userId)
                .name("ERROR")
                .email("N/A")
                .source("FALLBACK: " + ex.getClass().getSimpleName())
                .responseTimeMs(0)
                .build();
    }
}
