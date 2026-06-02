package com.example.jpa.config;

import com.example.jpa.service.ExternalApiService;
import com.example.jpa.service.SlackNotificationService;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Duration;
import java.util.function.Predicate;

@Slf4j
@Configuration
public class Resilience4jRetryConfig {

    private final SlackNotificationService slackNotificationService;

    public Resilience4jRetryConfig(SlackNotificationService slackNotificationService) {
        this.slackNotificationService = slackNotificationService;
    }

    /**
     * 공통 재시도 조건: 5xx 에러, 429 에러, I/O 에러도 재시도
     * 모든 Retry 설정에서 재사용됨
     */
    private Predicate<Throwable> shouldRetryPredicate() {
        return throwable -> {
            if (throwable instanceof HttpServerErrorException) {
                return true; // 5xx 에러 재시도
            }
            if (throwable instanceof HttpClientErrorException) {
                HttpClientErrorException ex = (HttpClientErrorException) throwable;
                return ex.getStatusCode().value() == 429; // 429 에러만 재시도
            }
            if (throwable instanceof org.springframework.web.client.ResourceAccessException) {
                return true; // I/O 에러도 재시도 (네트워크 장애 대응)
            }
            return false; // 그 외 에러는 재시도하지 않음
        };
    }

    /**
     * 외부 API 호출용 Retry 설정
     * - 최대 3회 시도
     * - 시도 간격: 1초
     * - 500번대 에러(5xx)와 429 에러만 재시도 (공통 조건 사용)
     */
    @Bean
    public RetryConfig externalApiRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryOnException(shouldRetryPredicate())  // 공통 재시도 조건 사용
                .build();
    }

    /**
     * 결제 API용 Retry 설정
     * - 최대 5회 시도 (더 중요한 API)
     * - 시도 간격: 2초 (더 느린 속도)
     * - 500번대 에러(5xx)와 429 에러만 재시도 (공통 조건 사용)
     */
    @Bean
    public RetryConfig paymentApiRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(5)
                .waitDuration(Duration.ofSeconds(2))
                .retryOnException(shouldRetryPredicate())  // 공통 재시도 조건 사용
                .build();
    }

    /**
     * 알림 API용 Retry 설정
     * - 최대 2회 시도 (덜 중요한 API)
     * - 시도 간격: 500ms (빠른 속도)
     * - 500번대 에러(5xx)와 429 에러만 재시도 (공통 조건 사용)
     */
    @Bean
    public RetryConfig notificationApiRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(shouldRetryPredicate())  // 공통 재시도 조건 사용
                .build();
    }

    /**
     * Retry 레지스트리 - 모든 Retry 설정을 등록
     * 하나의 Registry에서 여러 개의 Retry 인스턴스를 관리
     */
    @Bean
    public RetryRegistry retryRegistry(RetryConfig externalApiRetryConfig,
                                   RetryConfig paymentApiRetryConfig,
                                   RetryConfig notificationApiRetryConfig) {
        RetryRegistry registry = RetryRegistry.of(externalApiRetryConfig);
        // 각 이름에 대해 별도 설정 등록
        registry.addConfiguration("paymentApi", paymentApiRetryConfig);
        registry.addConfiguration("notificationApi", notificationApiRetryConfig);
        return registry;
    }

    /**
     * 외부 API용 Retry 빈
     */
    @Bean
    public Retry externalApiRetry(RetryRegistry retryRegistry) {
        Retry retry = retryRegistry.retry(ExternalApiService.RETRY_NAME);

        retry.getEventPublisher()
                .onError(event -> {
                    int currentAttempt = event.getNumberOfRetryAttempts() + 1;
                    int maxAttempts = retry.getRetryConfig().getMaxAttempts();

                    log.warn("Retry 실패 - 시도: {}/{}, 에러: {}",
                            currentAttempt, maxAttempts, event.getLastThrowable().getMessage());

                    if (currentAttempt >= maxAttempts) {
                        log.error("모든 재시도 실패! Slack 알림 전송 시작");
                        slackNotificationService.notifyRetryFailure(
                                "ExternalApiService",
                                event.getLastThrowable().getMessage(),
                                currentAttempt
                        );
                    }
                });

        return retry;
    }

    /**
     * 결제 API용 Retry 빈
     */
    @Bean
    public Retry paymentApiRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("paymentApi");
    }

    /**
     * 알림 API용 Retry 빈
     */
    @Bean
    public Retry notificationApiRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("notificationApi");
    }
}
