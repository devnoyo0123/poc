package com.example.ratelimit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "app.external-api")
@ConstructorBinding
public record ExternalApiProperties(String baseUrl, int timeoutMs, boolean simulated) {

  public ExternalApiProperties {
    if (baseUrl == null) {
      baseUrl = "https://httpbin.org";
    }
    if (timeoutMs <= 0) {
      timeoutMs = 5000;
    }
  }
}
