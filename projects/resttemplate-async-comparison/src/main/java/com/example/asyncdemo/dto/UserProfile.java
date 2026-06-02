package com.example.asyncdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    // 사용자 API 결과
    private Long userId;
    private String name;
    private String email;

    // 주문 API 결과
    private Integer totalOrders;
    private List<OrderSummary> recentOrders;

    // 결제 API 결과
    private Integer totalPayments;
    private Long totalAmount;

    // 포인트 API 결과
    private Integer currentPoints;

    // 메타 정보
    private long fetchTimeMs;
    private boolean hasError;
    private String errorMessage;

    // 각 API 호출 상태 추적
    @Builder.Default
    private ApiCallStatus userApiStatus = ApiCallStatus.SUCCESS;
    @Builder.Default
    private ApiCallStatus orderApiStatus = ApiCallStatus.SUCCESS;
    @Builder.Default
    private ApiCallStatus paymentApiStatus = ApiCallStatus.SUCCESS;
    @Builder.Default
    private ApiCallStatus pointApiStatus = ApiCallStatus.SUCCESS;

    /**
     * API 호출 상태 enum
     */
    public enum ApiCallStatus {
        SUCCESS,      // 성공
        FAILED,       // 실패
        FALLBACK,     // 실패 후 기본값 사용
        TIMEOUT       // 타임아웃
    }
}
