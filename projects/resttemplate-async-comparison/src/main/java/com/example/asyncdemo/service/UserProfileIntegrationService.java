package com.example.asyncdemo.service;

import com.example.asyncdemo.dto.ApiResponse;
import com.example.asyncdemo.dto.OrderSummary;
import com.example.asyncdemo.dto.UserProfile;
import com.example.asyncdemo.dto.UserProfile.ApiCallStatus;
import com.example.asyncdemo.entity.UserProfileEntity;
import com.example.asyncdemo.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 여러 API 병렬 호출 → 데이터 병합 → 영속화 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileIntegrationService {

    private final ExternalApiIntegrationService apiService;
    private final UserProfileRepository repository;

    /**
     * 순차적으로 여러 API 호출 후 데이터 병합 및 저장
     */
    @Transactional
    public UserProfile fetchAndSaveSequential(Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("=== 순차 통합 호출 시작 === userId: {}", userId);

        // 1. 사용자 API 호출
        ApiResponse.UserInfo userInfo = apiService.fetchUserInfo(userId).join();

        // 2. 주문 API 호출
        ApiResponse.OrderInfo orderInfo = apiService.fetchOrderInfo(userId).join();

        // 3. 결제 API 호출
        ApiResponse.PaymentInfo paymentInfo = apiService.fetchPaymentInfo(userId).join();

        // 4. 포인트 API 호출
        ApiResponse.PointInfo pointInfo = apiService.fetchPointInfo(userId).join();

        // 데이터 병합
        UserProfile profile = mergeData(userId, userInfo, orderInfo, paymentInfo, pointInfo);
        profile.setFetchTimeMs(System.currentTimeMillis() - startTime);

        // DB 저장
        saveToDatabase(profile);

        log.info("=== 순차 통합 호출 완료 === 총 소요시간: {}ms", profile.getFetchTimeMs());
        return profile;
    }

    /**
     * 병렬로 여러 API 호출 후 데이터 병합 및 저장
     * allOf + thenApply 패턴 사용
     */
    @Transactional
    public UserProfile fetchAndSaveParallel(Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("=== 병렬 통합 호출 시작 === userId: {}", userId);

        // 모든 API 호출을 동시에 시작
        CompletableFuture<ApiResponse.UserInfo> userInfoFuture = apiService.fetchUserInfo(userId);
        CompletableFuture<ApiResponse.OrderInfo> orderInfoFuture = apiService.fetchOrderInfo(userId);
        CompletableFuture<ApiResponse.PaymentInfo> paymentInfoFuture = apiService.fetchPaymentInfo(userId);
        CompletableFuture<ApiResponse.PointInfo> pointInfoFuture = apiService.fetchPointInfo(userId);

        // 모든 Future가 완료될 때까지 대기 후 병합
        UserProfile profile = CompletableFuture.allOf(
                        userInfoFuture, orderInfoFuture, paymentInfoFuture, pointInfoFuture)
                .thenApply(v -> {
                    // 모든 API 완료 후 결과 병합
                    return mergeData(
                            userId,
                            userInfoFuture.join(),
                            orderInfoFuture.join(),
                            paymentInfoFuture.join(),
                            pointInfoFuture.join()
                    );
                })
                .join();

        profile.setFetchTimeMs(System.currentTimeMillis() - startTime);

        // DB 저장
        saveToDatabase(profile);

        log.info("=== 병렬 통합 호출 완료 === 총 소요시간: {}ms", profile.getFetchTimeMs());
        return profile;
    }

    /**
     * 여러 API 결과를 하나의 객체로 병합
     */
    private UserProfile mergeData(
            Long userId,
            ApiResponse.UserInfo userInfo,
            ApiResponse.OrderInfo orderInfo,
            ApiResponse.PaymentInfo paymentInfo,
            ApiResponse.PointInfo pointInfo) {

        return UserProfile.builder()
                .userId(userId)
                .name(userInfo.getName())
                .email(userInfo.getEmail())
                .totalOrders(orderInfo.getTotalCount())
                .recentOrders(orderInfo.getOrders())
                .totalPayments(paymentInfo.getTotalCount())
                .totalAmount(paymentInfo.getTotalAmount())
                .currentPoints(pointInfo.getCurrentPoints())
                .hasError(false)
                .build();
    }

    /**
     * DB에 저장 (Upsert)
     */
    private void saveToDatabase(UserProfile profile) {
        UserProfileEntity entity = repository.findByUserId(profile.getUserId())
                .orElseGet(() -> UserProfileEntity.builder().userId(profile.getUserId()).build());

        entity.setName(profile.getName());
        entity.setEmail(profile.getEmail());
        entity.setTotalOrders(profile.getTotalOrders());
        entity.setTotalPayments(profile.getTotalPayments());
        entity.setTotalAmount(profile.getTotalAmount());
        entity.setCurrentPoints(profile.getCurrentPoints());

        repository.save(entity);
        log.info("DB 저장 완료 - userId: {}", profile.getUserId());
    }

    /**
     * 성능 비교
     */
    public ComparisonResult compareIntegration(Long userId) {
        log.info("========== 통합 API 성능 비교 시작 ==========");

        // 순차
        UserProfile sequentialResult = fetchAndSaveSequential(userId);
        long sequentialTime = sequentialResult.getFetchTimeMs();

        // 병렬
        UserProfile parallelResult = fetchAndSaveParallel(userId);
        long parallelTime = parallelResult.getFetchTimeMs();

        double improvementRate = ((double) (sequentialTime - parallelTime) / sequentialTime) * 100;

        log.info("========== 통합 API 성능 비교 완료 ==========");
        log.info("순차: {}ms, 병렬: {}ms, 개선율: {}%", sequentialTime, parallelTime,
                String.format("%.2f%%", improvementRate));

        return ComparisonResult.builder()
                .executionType("INTEGRATION_COMPARISON")
                .sequentialTimeMs(sequentialTime)
                .parallelTimeMs(parallelTime)
                .savedTimeMs(sequentialTime - parallelTime)
                .improvementRate(String.format("%.2f%%", improvementRate))
                .build();
    }

    // 내부 DTO
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ComparisonResult {
        private String executionType;
        private long sequentialTimeMs;
        private long parallelTimeMs;
        private long savedTimeMs;
        private String improvementRate;
    }

    // ==================== 에러 처리 패턴 ====================

    /**
     * 각 API 호출 결과를 래핑하는 클래스
     * 성공/실패 여부와 에러 메시지를 함께 전달
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class ApiResult<T> {
        private T data;
        private boolean success;
        private String errorMessage;
        private Throwable exception;

        static <T> ApiResult<T> success(T data) {
            return ApiResult.<T>builder().data(data).success(true).build();
        }

        static <T> ApiResult<T> failure(Throwable ex) {
            return ApiResult.<T>builder()
                    .success(false)
                    .errorMessage(ex.getMessage())
                    .exception(ex)
                    .build();
        }
    }

    /**
     * 여러 API 병렬 호출 + 개별 에러 처리 + 상태 추적
     *
     * 핵심 패턴:
     * 1. 각 Future에 handle()을 사용하여 성공/실패를 ApiResult로 래핑
     * 2. allOf로 모든 Future 완료 대기
     * 3. 결과 병합 시 각 API의 성공/실패 상태 추적
     */
    @Transactional
    public UserProfile fetchAndSaveWithErrorTracking(Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("=== 에러 추적 병렬 호출 시작 === userId: {}", userId);

        // 1. 각 API 호출 + handle()로 결과 래핑
        CompletableFuture<ApiResult<ApiResponse.UserInfo>> userFuture =
                apiService.fetchUserInfo(userId)
                        .handle((result, ex) -> ex == null
                                ? ApiResult.success(result)
                                : ApiResult.<ApiResponse.UserInfo>failure(ex));

        CompletableFuture<ApiResult<ApiResponse.OrderInfo>> orderFuture =
                apiService.fetchOrderInfo(userId)
                        .handle((result, ex) -> ex == null
                                ? ApiResult.success(result)
                                : ApiResult.<ApiResponse.OrderInfo>failure(ex));

        CompletableFuture<ApiResult<ApiResponse.PaymentInfo>> paymentFuture =
                apiService.fetchPaymentInfo(userId)
                        .handle((result, ex) -> ex == null
                                ? ApiResult.success(result)
                                : ApiResult.<ApiResponse.PaymentInfo>failure(ex));

        CompletableFuture<ApiResult<ApiResponse.PointInfo>> pointFuture =
                apiService.fetchPointInfo(userId)
                        .handle((result, ex) -> ex == null
                                ? ApiResult.success(result)
                                : ApiResult.<ApiResponse.PointInfo>failure(ex));

        // 2. 모든 Future 완료 대기 (handle() 덕분에 실패해도 완료로 처리됨)
        CompletableFuture.allOf(userFuture, orderFuture, paymentFuture, pointFuture).join();

        // 3. 결과 추출
        ApiResult<ApiResponse.UserInfo> userResult = userFuture.join();
        ApiResult<ApiResponse.OrderInfo> orderResult = orderFuture.join();
        ApiResult<ApiResponse.PaymentInfo> paymentResult = paymentFuture.join();
        ApiResult<ApiResponse.PointInfo> pointResult = pointFuture.join();

        // 4. 에러 로깅
        List<String> errors = new ArrayList<>();
        if (!userResult.isSuccess()) {
            log.error("사용자 API 실패: {}", userResult.getErrorMessage());
            errors.add("USER_API: " + userResult.getErrorMessage());
        }
        if (!orderResult.isSuccess()) {
            log.error("주문 API 실패: {}", orderResult.getErrorMessage());
            errors.add("ORDER_API: " + orderResult.getErrorMessage());
        }
        if (!paymentResult.isSuccess()) {
            log.error("결제 API 실패: {}", paymentResult.getErrorMessage());
            errors.add("PAYMENT_API: " + paymentResult.getErrorMessage());
        }
        if (!pointResult.isSuccess()) {
            log.error("포인트 API 실패: {}", pointResult.getErrorMessage());
            errors.add("POINT_API: " + pointResult.getErrorMessage());
        }

        // 5. 성공한 API 결과만으로 병합 (실패한 API는 기본값 사용)
        UserProfile profile = mergeWithFallback(
                userId, userResult, orderResult, paymentResult, pointResult);

        profile.setFetchTimeMs(System.currentTimeMillis() - startTime);
        profile.setHasError(!errors.isEmpty());
        profile.setErrorMessage(errors.isEmpty() ? null : String.join(", ", errors));

        // 6. DB 저장 (부분 성공이어도 저장)
        saveToDatabase(profile);

        log.info("=== 에러 추적 병렬 호출 완료 === 소요시간: {}ms, 에러: {}",
                profile.getFetchTimeMs(), errors.size());
        return profile;
    }

    /**
     * 성공한 API 결과로 병합, 실패한 API는 기본값 사용
     */
    private UserProfile mergeWithFallback(
            Long userId,
            ApiResult<ApiResponse.UserInfo> userResult,
            ApiResult<ApiResponse.OrderInfo> orderResult,
            ApiResult<ApiResponse.PaymentInfo> paymentResult,
            ApiResult<ApiResponse.PointInfo> pointResult) {

        // 사용자 API: 실패 시 기본값
        String name = "UNKNOWN";
        String email = "N/A";
        ApiCallStatus userStatus = ApiCallStatus.FALLBACK;
        if (userResult.isSuccess() && userResult.getData() != null) {
            name = userResult.getData().getName();
            email = userResult.getData().getEmail();
            userStatus = ApiCallStatus.SUCCESS;
        }

        // 주문 API: 실패 시 기본값
        Integer totalOrders = 0;
        List<OrderSummary> recentOrders = List.of();
        ApiCallStatus orderStatus = ApiCallStatus.FALLBACK;
        if (orderResult.isSuccess() && orderResult.getData() != null) {
            totalOrders = orderResult.getData().getTotalCount();
            recentOrders = orderResult.getData().getOrders();
            orderStatus = ApiCallStatus.SUCCESS;
        }

        // 결제 API: 실패 시 기본값
        Integer totalPayments = 0;
        Long totalAmount = 0L;
        ApiCallStatus paymentStatus = ApiCallStatus.FALLBACK;
        if (paymentResult.isSuccess() && paymentResult.getData() != null) {
            totalPayments = paymentResult.getData().getTotalCount();
            totalAmount = paymentResult.getData().getTotalAmount();
            paymentStatus = ApiCallStatus.SUCCESS;
        }

        // 포인트 API: 실패 시 기본값
        Integer currentPoints = 0;
        ApiCallStatus pointStatus = ApiCallStatus.FALLBACK;
        if (pointResult.isSuccess() && pointResult.getData() != null) {
            currentPoints = pointResult.getData().getCurrentPoints();
            pointStatus = ApiCallStatus.SUCCESS;
        }

        return UserProfile.builder()
                .userId(userId)
                .name(name)
                .email(email)
                .totalOrders(totalOrders)
                .recentOrders(recentOrders)
                .totalPayments(totalPayments)
                .totalAmount(totalAmount)
                .currentPoints(currentPoints)
                .userApiStatus(userStatus)
                .orderApiStatus(orderStatus)
                .paymentApiStatus(paymentStatus)
                .pointApiStatus(pointStatus)
                .build();
    }
}
