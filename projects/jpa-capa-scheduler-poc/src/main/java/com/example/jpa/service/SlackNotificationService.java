package com.example.jpa.service;

import com.slack.api.Slack;
import com.slack.api.webhook.WebhookResponse;
import com.slack.api.webhook.Payload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationService {

    @Value("${slack.webhook.url:}")
    private String slackWebhookUrl;

    @Value("${slack.enabled:false}")
    private boolean slackEnabled;

    private final Slack slack = Slack.getInstance();

    /**
     * 외부 API 재시도 실패 알림 전송
     */
    public void notifyRetryFailure(String serviceName, String errorMessage, int attemptCount) {
        log.info("[DEBUG] Slack 설정 확인 - enabled={}, webhookUrl='{}'", slackEnabled, slackWebhookUrl);

        if (!slackEnabled || slackWebhookUrl.isEmpty()) {
            log.warn("Slack 알림이 비활성화되어 있거나 Webhook URL이 설정되지 않았습니다.");
            log.warn("[DEBUG] slackEnabled={}, url.isEmpty()={}", slackEnabled, slackWebhookUrl.isEmpty());
            return;
        }

        try {
            Payload payload = createRetryFailurePayload(serviceName, errorMessage, attemptCount);
            WebhookResponse response = slack.send(slackWebhookUrl, payload);

            if (response.getCode() == 200) {
                log.info("Slack 알림 전송 성공");
            } else {
                log.warn("Slack 알림 전송 결과: code={}, body={}", response.getCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * Retry 실패 메시지 페이로드 생성
     */
    private Payload createRetryFailurePayload(String serviceName, String errorMessage, int attemptCount) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("🚨 *외부 API 호출 최종 실패*\n\n");
        sb.append("*서비스:* ").append(serviceName).append("\n");
        sb.append("*실패 시간:* ").append(timestamp).append("\n");
        sb.append("*시도 횟수:* ").append(attemptCount).append("/3\n");
        sb.append("*에러 메시지:* `").append(errorMessage != null ? errorMessage : "Unknown error").append("`\n\n");
        sb.append("━\n");
        sb.append("_JPA Capa Scheduler POC_");

        return Payload.builder()
                .text(String.format("🚨 [API 재시도 실패] %s", serviceName))
                .build();
    }
}
