package com.example.ratelimit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "app.backpressure")
@ConstructorBinding
public record BackpressureProperties(String strategy, int queueCapacity) {

  public BackpressureProperties {
    if (strategy == null || strategy.isBlank()) {
      strategy = "rxjava";
    }
    if (queueCapacity <= 0) {
      queueCapacity = 100;
    }
  }

  public boolean isBlockingQueue() {
    return "blocking-queue".equalsIgnoreCase(strategy);
  }

  public boolean isRxjava() {
    return "rxjava".equalsIgnoreCase(strategy);
  }
}
