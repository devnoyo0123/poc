package com.poc.asynccontext.controller;

import com.poc.asynccontext.dto.AggregatedResponse;
import com.poc.asynccontext.dto.ResponseA;
import com.poc.asynccontext.dto.ResponseB;
import com.poc.asynccontext.dto.ResponseC;
import com.poc.asynccontext.service.ExternalServiceA;
import com.poc.asynccontext.service.ExternalServiceB;
import com.poc.asynccontext.service.ExternalServiceC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

/**
 * 반환 타입 = Callable.
 * 메커니즘: Spring MVC가 startAsync() 호출 후 Callable을 "MVC TaskExecutor"의 별도 스레드에서 실행.
 *          Tomcat 워커는 즉시 반납됨. Callable 안의 코드(아래 join 포함)는 워커가 아닌 다른 스레드에서 돎.
 * 특징: 블로킹(join)을 그대로 써도 됨 — 어차피 워커가 아닌 별도 스레드라 워커는 안 묶임.
 *       단, 그 "별도 스레드 풀(MVC TaskExecutor)"이 새 병목이 될 수 있음(기본 무제한 SimpleAsyncTaskExecutor).
 */
@RestController
@RequestMapping("/callable")
public class CallableController {

    private final ExternalServiceA serviceA;
    private final ExternalServiceB serviceB;
    private final ExternalServiceC serviceC;

    public CallableController(ExternalServiceA serviceA,
                              ExternalServiceB serviceB,
                              ExternalServiceC serviceC) {
        this.serviceA = serviceA;
        this.serviceB = serviceB;
        this.serviceC = serviceC;
    }

    @GetMapping
    public Callable<AggregatedResponse> callable() {
        long start = System.currentTimeMillis();

        // 이 람다 전체가 Tomcat 워커가 아닌 MVC TaskExecutor 스레드에서 실행됨
        return () -> {
            CompletableFuture<ResponseA> fa = serviceA.callAsync();
            CompletableFuture<ResponseB> fb = serviceB.callAsync();
            CompletableFuture<ResponseC> fc = serviceC.callAsync();

            CompletableFuture.allOf(fa, fb, fc).join();  // 여기서 블록돼도 워커 아님

            long elapsed = System.currentTimeMillis() - start;
            return new AggregatedResponse(fa.join(), fb.join(), fc.join(), elapsed, "CALLABLE");
        };
    }
}