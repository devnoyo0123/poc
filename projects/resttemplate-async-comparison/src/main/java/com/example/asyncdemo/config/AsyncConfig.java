package com.example.asyncdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // CompletableFuture는 ExternalApiService에서 별도 Executor 사용
    // Java 17에서는 Virtual Thread 미지원이므로 일반 스레드 풀 사용
}
