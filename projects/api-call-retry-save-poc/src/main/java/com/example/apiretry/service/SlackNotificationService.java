package com.example.apiretry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlackNotificationService {

    private final RestTemplate restTemplate;

    @Value("${slack.webhook-url}")
    private String slackWebhookUrl;

    @Value("${slack.enabled:true}")
    private boolean slackEnabled;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 즉시 실패 알림 (재시도 없이 바로 실패)
     */
    public void notifyImmediateFailure(String endpoint, String errorCode, String errorMessage) {
        String message = buildImmediateFailureMessage(endpoint, errorCode, errorMessage);
        sendSlackMessage(message);
    }

    /**
     * 재시도 실패 알림 (각 시도마다)
     */
    public void notifyRetryFailure(String endpoint, int attemptCount, String errorCode, String errorMessage) {
        String message = buildRetryFailureMessage(endpoint, attemptCount, errorCode, errorMessage);
        sendSlackMessage(message);
    }

    /**
     * 최종 실패 알림 (max-attempts 초과)
     */
    public void notifyFinalFailure(String endpoint, int attemptCount, String errorMessage) {
        String message = buildFinalFailureMessage(endpoint, attemptCount, errorMessage);
        sendSlackMessage(message);
    }

    /**
     * 성공 알림 (선택 사항)
     */
    public void notifySuccess(String endpoint, int attemptCount) {
        String message = buildSuccessMessage(endpoint, attemptCount);
        sendSlackMessage(message);
    }

    /**
     * 외부 API 예외 알림 (ExternalApiException용)
     * 재시도 실패와 즉시 실패를 자동으로 구분하여 알림
     */
    public void notifyExternalApiFailure(String serviceName, String url, int attemptCount,
                                        boolean retryExhausted, String errorCode) {
        String message = buildExternalApiFailureMessage(serviceName, url, attemptCount, retryExhausted, errorCode);
        sendSlackMessage(message);
    }

    private String buildImmediateFailureMessage(String endpoint, String errorCode, String errorMessage) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        return String.format(
            "🚨 *API 즉시 실패 알림*\n" +
            "--------------------------------\n" +
            "*시각*: %s\n" +
            "*엔드포인트*: %s\n" +
            "*실패 유형*: 즉시 실패 (재시도 안 함)\n" +
            "*에러 코드*: %s\n" +
            "*에러 메시지*: %s\n" +
            "--------------------------------",
            timestamp, endpoint, errorCode, errorMessage.substring(0, Math.min(500, errorMessage.length()))
        );
    }

    private String buildRetryFailureMessage(String endpoint, int attemptCount, String errorCode, String errorMessage) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        return String.format(
            "🔄 *API 재시도 실패 알림*\n" +
            "--------------------------------\n" +
            "*시각*: %s\n" +
            "*엔드포인트*: %s\n" +
            "*실패 유형*: 재시도 실패\n" +
            "*시도 횟수*: %d회\n" +
            "*에러 코드*: %s\n" +
            "*에러 메시지*: %s\n" +
            "--------------------------------",
            timestamp, endpoint, attemptCount, errorCode, errorMessage.substring(0, Math.min(500, errorMessage.length()))
        );
    }

    private String buildFinalFailureMessage(String endpoint, int attemptCount, String errorMessage) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        return String.format(
            "❌ *API 최종 실패 알림*\n" +
            "--------------------------------\n" +
            "*시각*: %s\n" +
            "*엔드포인트*: %s\n" +
            "*실패 유형*: 최종 실패 (최대 시도 초과)\n" +
            "*시도 횟수*: %d회\n" +
            "*에러 메시지*: %s\n" +
            "--------------------------------",
            timestamp, endpoint, attemptCount, errorMessage.substring(0, Math.min(500, errorMessage.length()))
        );
    }

    private String buildSuccessMessage(String endpoint, int attemptCount) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        return String.format(
            "✅ *API 성공 알림*\n" +
            "--------------------------------\n" +
            "*시각*: %s\n" +
            "*엔드포인트*: %s\n" +
            "*시도 횟수*: %d회\n" +
            "--------------------------------",
            timestamp, endpoint, attemptCount
        );
    }

    private String buildExternalApiFailureMessage(String serviceName, String url, int attemptCount,
                                                  boolean retryExhausted, String errorCode) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        if (retryExhausted) {
            // 재시도 실패 (더 심각)
            return String.format(
                "🔥 *[%s] 외부 API 재시도 실패*\n" +
                "================================\n" +
                "*⏰ 시각*: %s\n" +
                "*🔗 URL*: %s\n" +
                "*🔄 시도 횟수*: %d회\n" +
                "*❌ 마지막 에러*: %s\n" +
                "*📌 실패 유형*: 재시도 소진 (최대 시도 초과)\n" +
                "================================\n" +
                "⚠️ *운영 조치 필요*: %s 서비스 상태 확인 또는 수동 처리",
                serviceName, timestamp, url, attemptCount, errorCode, serviceName
            );
        } else {
            // 즉시 실패 (덜 심각)
            return String.format(
                "⚠️ *[%s] 외부 API 호출 실패*\n" +
                "--------------------------------\n" +
                "*⏰ 시각*: %s\n" +
                "*🔗 URL*: %s\n" +
                "*❌ 에러 코드*: %s\n" +
                "*📌 실패 유형*: 즉시 실패 (재시도 불필요)\n" +
                "--------------------------------\n" +
                "💡 *참고*: 클라이언트 에러 (4xx) 또는 재시도 불가 오류",
                serviceName, timestamp, url, errorCode
            );
        }
    }

    /**
     * Slack으로 메시지 전송
     * 현재는 로그만 출력 (실제 Slack 전송은 주석처리됨)
     */
    private void sendSlackMessage(String message) {
        log.info("📤 Slack 알림 (현재는 로그만 출력)");
        log.info("{}", message);

        // 실제 Slack 전송이 필요할 때 아래 코드 활성화
        if (!slackEnabled) {
            log.debug("Slack notification is disabled");
            return;
        }

        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("text", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForEntity(slackWebhookUrl, request, String.class);

            log.info("Slack notification sent successfully");
        } catch (Exception e) {
            log.error("Failed to send Slack notification: {}", e.getMessage());
        }

    }
}
