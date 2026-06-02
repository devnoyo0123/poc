package com.example.ratelimit.client;

import com.example.ratelimit.config.ExternalApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ExternalApiClient {

  private final RestTemplate rest;
  private final String baseUrl;

  public ExternalApiClient(RestTemplateBuilder builder, ExternalApiProperties props) {
    this.baseUrl = props.baseUrl();
    this.rest =
        builder
            .rootUri(baseUrl)
            .setConnectTimeout(Duration.ofMillis(props.timeoutMs()))
            .setReadTimeout(Duration.ofMillis(props.timeoutMs()))
            .build();
  }

  /** 외부 API 호출 (rate limit 적용 전 호출부에서 제한) */
  public JsonNode call(String path) {
    return rest.getForObject(path, JsonNode.class);
  }

  public String getBaseUrl() {
    return baseUrl;
  }
}
