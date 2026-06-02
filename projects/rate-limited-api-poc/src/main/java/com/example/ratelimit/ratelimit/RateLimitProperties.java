package com.example.ratelimit.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "app.rate-limit")
@ConstructorBinding
public record RateLimitProperties(String key, int permitsPerSecond, int capacity) {

  public RateLimitProperties {
    if (key == null) {
      key = "external-api:ratelimit";
    }
    if (permitsPerSecond <= 0) {
      permitsPerSecond = 2;
    }
    if (capacity <= 0) {
      capacity = 2;
    }
  }
}
