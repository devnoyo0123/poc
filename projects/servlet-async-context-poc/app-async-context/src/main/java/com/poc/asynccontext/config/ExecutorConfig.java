package com.poc.asynccontext.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 서비스별 전용 스레드 풀(bulkhead).
 * 의도: A/B/C 외부 호출을 독립 풀로 격리.
 *       한 외부 API가 느려져 풀이 포화돼도 다른 서비스 호출은 영향 안 받음(장애 전파 차단).
 *       각 풀은 bounded — 무제한 cached와 달리 포화/격리 동작을 실제로 관찰 가능.
 */
@Configuration
public class ExecutorConfig {

    @Bean("executorA")
    public Executor executorA() {
        return boundedPool("ext-a-");
    }

    @Bean("executorB")
    public Executor executorB() {
        return boundedPool("ext-b-");
    }

    @Bean("executorC")
    public Executor executorC() {
        return boundedPool("ext-c-");
    }

    private Executor boundedPool(String namePrefix) {
        return new ThreadPoolExecutor(
                10,                                  // core
                10,                                  // max (포화 한계 = bulkhead 크기)
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),      // 초과 요청 대기 큐
                namedDaemonFactory(namePrefix),
                new ThreadPoolExecutor.CallerRunsPolicy()  // 큐까지 차면 호출 스레드가 직접 실행(백프레셔)
        );
    }

    private ThreadFactory namedDaemonFactory(String namePrefix) {
        AtomicInteger seq = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r);
            t.setName(namePrefix + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
    }
}