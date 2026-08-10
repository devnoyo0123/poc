package com.poc.vtjpa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecutorConfig {

    @Bean("jpaPlatformExecutor")
    public Executor jpaPlatformExecutor() {
        return new ThreadPoolExecutor(
                50, 50,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                namedDaemonPlatformFactory("jpa-platform-"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean("jpaVirtualExecutor")
    public Executor jpaVirtualExecutor() {
        ThreadFactory factory = Thread.ofVirtual()
                .name("jpa-virtual-", 0)
                .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

    private ThreadFactory namedDaemonPlatformFactory(String namePrefix) {
        AtomicInteger seq = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r);
            t.setName(namePrefix + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }
}
