package com.example.apiretry.exception;

import lombok.Getter;

/**
 * 외부 API 연동 실패 예외
 * 모든 외부 서비스 (ESM, NAVER, KAKAO 등) API 호출 실패 시 사용
 *
 * <p>재사용 가능한 설계:
 * - serviceName 필드로 서비스 구분 (ESM, NAVER, KAKAO 등)
 * - retryExhausted 필드로 재시도 실패 여부 구분
 * - 확장성: 새로운 외부 서비스 추가 시 코드 재사용 가능
 * </p>
 */
@Getter
public class ExternalApiException extends RuntimeException {

    private final String serviceName;      // 외부 서비스명 (예: "ESM", "NAVER", "KAKAO")
    private final String url;              // 호출 실패한 API URL
    private final int attemptCount;        // 총 시도 횟수
    private final boolean retryExhausted;  // true: 재시도 후 실패, false: 즉시 실패
    private final String errorCode;        // 마지막 에러 코드 (예: "500", "429", "400")
    private final boolean alreadySaved;    // DB 저장 완료 여부 (중복 저장 방지)

    /**
     * 전체 생성자
     */
    public ExternalApiException(
            String serviceName,
            String message,
            String url,
            int attemptCount,
            boolean retryExhausted,
            String errorCode,
            boolean alreadySaved
    ) {
        super(buildMessage(serviceName, message, retryExhausted));
        this.serviceName = serviceName;
        this.url = url;
        this.attemptCount = attemptCount;
        this.retryExhausted = retryExhausted;
        this.errorCode = errorCode;
        this.alreadySaved = alreadySaved;
    }

    /**
     * 재시도 실패용 편의 생성자
     * 5xx, 429, timeout 등 재시도 가능한 에러를 최대 횟수만큼 시도했으나 실패
     *
     * @param serviceName 서비스명 (예: "ESM", "NAVER")
     * @param url 호출 실패한 URL
     * @param attemptCount 총 시도 횟수
     * @param lastErrorCode 마지막 에러 코드
     * @return ExternalApiException 인스턴스
     */
    public static ExternalApiException retryExhausted(
            String serviceName,
            String url,
            int attemptCount,
            String lastErrorCode
    ) {
        return new ExternalApiException(
                serviceName,
                String.format("재시도 %d회 후 최종 실패", attemptCount),
                url,
                attemptCount,
                true,  // retryExhausted = true
                lastErrorCode,
                false
        );
    }

    /**
     * 즉시 실패용 편의 생성자
     * 4xx 클라이언트 에러 등 재시도가 불필요한 에러
     *
     * @param serviceName 서비스명 (예: "ESM", "NAVER")
     * @param url 호출 실패한 URL
     * @param errorCode 에러 코드 (예: "400", "404")
     * @param errorMessage 에러 메시지
     * @return ExternalApiException 인스턴스
     */
    public static ExternalApiException callFailed(
            String serviceName,
            String url,
            String errorCode,
            String errorMessage
    ) {
        return new ExternalApiException(
                serviceName,
                errorMessage,
                url,
                1,     // attemptCount = 1
                false, // retryExhausted = false
                errorCode,
                false
        );
    }

    /**
     * DB 저장 완료 플래그 포함 생성자
     */
    public static ExternalApiException retryExhaustedWithSaved(
            String serviceName,
            String url,
            int attemptCount,
            String lastErrorCode
    ) {
        return new ExternalApiException(
                serviceName,
                String.format("재시도 %d회 후 최종 실패", attemptCount),
                url,
                attemptCount,
                true,
                lastErrorCode,
                true  // alreadySaved = true
        );
    }

    /**
     * 예외 메시지 생성
     */
    private static String buildMessage(String serviceName, String message, boolean retryExhausted) {
        String prefix = retryExhausted ? "🔄 재시도 실패" : "⚠️ 즉시 실패";
        return String.format("[%s] %s - %s", serviceName, prefix, message);
    }

    /**
     * 재시도 실패 여부 판단 (편의 메서드)
     */
    public boolean isRetryExhausted() {
        return retryExhausted;
    }

    /**
     * 즉시 실패 여부 판단 (편의 메서드)
     */
    public boolean isImmediateFailure() {
        return !retryExhausted;
    }
}
