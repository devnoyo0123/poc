package com.example.ratelimit;

import com.example.ratelimit.config.BackpressureProperties;
import com.example.ratelimit.config.ExternalApiProperties;
import com.example.ratelimit.ratelimit.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
  RateLimitProperties.class,
  BackpressureProperties.class,
  ExternalApiProperties.class
})
public class RateLimitedApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(RateLimitedApiApplication.class, args);
  }
}
