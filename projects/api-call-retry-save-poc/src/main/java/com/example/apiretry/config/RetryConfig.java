package com.example.apiretry.config;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;


@Configuration
@Slf4j
public class RetryConfig {

    @Bean
    public RetryRegistry retryRegistry() {
        io.github.resilience4j.retry.RetryConfig config = io.github.resilience4j.retry.RetryConfig.custom()
            .maxAttempts(3)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(1000L, 2.0))
            .retryOnException(e -> e instanceof HttpServerErrorException
                || e instanceof HttpClientErrorException.TooManyRequests
                || e instanceof ResourceAccessException
            )
            .failAfterMaxAttempts(true)
            .build();

        return RetryRegistry.of(config);
    }

    @Bean
    public Retry apiCallRetryV2(RetryRegistry registry) {
        return registry.retry("apiCallRetryV2");
    }

    @Bean
    public Retry apiCallRetry(RetryRegistry registry) {
        return registry.retry("apiCallRetry");
    }
}
