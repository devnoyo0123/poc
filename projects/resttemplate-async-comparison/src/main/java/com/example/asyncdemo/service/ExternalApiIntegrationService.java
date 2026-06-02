package com.example.asyncdemo.service;

import com.example.asyncdemo.dto.ApiResponse;
import com.example.asyncdemo.dto.OrderSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 여러 외부 API를 호출하는 서비스
 * 각 API는 서로 다른 도메인 (사용자, 주문, 결제, 포인트)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${external-api.delay-ms:1000}")
    private long delayMs;

    private final Executor executor = Executors.newFixedThreadPool(10);

    // ==================== 사용자 API ====================

    public CompletableFuture<ApiResponse.UserInfo> fetchUserInfo(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            simulateDelay("사용자 API");
            return ApiResponse.UserInfo.builder()
                    .userId(userId)
                    .name("홍길동")
                    .email("hong@example.com")
                    .build();
        }, executor).exceptionally(ex -> {
            log.error("사용자 API 호출 실패: {}", ex.getMessage());
            return ApiResponse.UserInfo.builder()
                    .userId(userId)
                    .name("UNKNOWN")
                    .email("N/A")
                    .build();
        });
    }

    // ==================== 주문 API ====================

    public CompletableFuture<ApiResponse.OrderInfo> fetchOrderInfo(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            simulateDelay("주문 API");
            List<OrderSummary> orders = Arrays.asList(
                    OrderSummary.builder().orderId(1L).productName("노트북").quantity(1).price(1500000L).build(),
                    OrderSummary.builder().orderId(2L).productName("마우스").quantity(2).price(50000L).build()
            );
            return ApiResponse.OrderInfo.builder()
                    .totalCount(5)
                    .orders(orders)
                    .build();
        }, executor).exceptionally(ex -> {
            log.error("주문 API 호출 실패: {}", ex.getMessage());
            return ApiResponse.OrderInfo.builder()
                    .totalCount(0)
                    .orders(List.of())
                    .build();
        });
    }

    // ==================== 결제 API ====================

    public CompletableFuture<ApiResponse.PaymentInfo> fetchPaymentInfo(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            simulateDelay("결제 API");
            return ApiResponse.PaymentInfo.builder()
                    .totalCount(5)
                    .totalAmount(1650000L)
                    .build();
        }, executor).exceptionally(ex -> {
            log.error("결제 API 호출 실패: {}", ex.getMessage());
            return ApiResponse.PaymentInfo.builder()
                    .totalCount(0)
                    .totalAmount(0L)
                    .build();
        });
    }

    // ==================== 포인트 API ====================

    public CompletableFuture<ApiResponse.PointInfo> fetchPointInfo(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            simulateDelay("포인트 API");
            return ApiResponse.PointInfo.builder()
                    .currentPoints(5000)
                    .build();
        }, executor).exceptionally(ex -> {
            log.error("포인트 API 호출 실패: {}", ex.getMessage());
            return ApiResponse.PointInfo.builder()
                    .currentPoints(0)
                    .build();
        });
    }

    // ==================== 유틸리티 ====================

    private void simulateDelay(String apiName) {
        log.info("{} 호출 시작...", apiName);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(apiName + " 호출 중 인터럽트", e);
        }
        log.info("{} 호출 완료", apiName);
    }
}
