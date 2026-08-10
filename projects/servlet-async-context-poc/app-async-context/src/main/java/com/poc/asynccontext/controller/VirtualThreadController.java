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

import java.util.concurrent.CompletableFuture;

/**
 * 반환 타입 = 완성 객체(블로킹 스타일). /blocking 과 코드 동일.
 * 차이: application.yml 의 spring.threads.virtual.enabled=true 가 켜져 있으면
 *       이 요청을 처리하는 "Tomcat 워커"가 플랫폼 스레드가 아니라 가상 스레드(Virtual Thread)임.
 *
 * 가상 스레드 원리: join()으로 블록되면 가상 스레드는 "캐리어(실제 OS 스레드)에서 떨어져 나와(unmount)" 대기.
 *                  OS 스레드는 그동안 다른 가상 스레드를 실행. 즉 startAsync() 없이도 OS 스레드는 안 묶임.
 * 결론: join()을 써도(=동기 코드처럼 짜도) /async 만큼 빠름. 콜백/Future 반환 불필요.
 */
@RestController
@RequestMapping("/virtual")
public class VirtualThreadController {

    private final ExternalServiceA serviceA;
    private final ExternalServiceB serviceB;
    private final ExternalServiceC serviceC;

    public VirtualThreadController(ExternalServiceA serviceA,
                                   ExternalServiceB serviceB,
                                   ExternalServiceC serviceC) {
        this.serviceA = serviceA;
        this.serviceB = serviceB;
        this.serviceC = serviceC;
    }

    @GetMapping
    public AggregatedResponse virtual() {
        long start = System.currentTimeMillis();

        CompletableFuture<ResponseA> fa = serviceA.callAsync();
        CompletableFuture<ResponseB> fb = serviceB.callAsync();
        CompletableFuture<ResponseC> fc = serviceC.callAsync();

        CompletableFuture.allOf(fa, fb, fc).join();  // 블록 — 하지만 가상 스레드라 OS 스레드 안 묶임

        long elapsed = System.currentTimeMillis() - start;
        return new AggregatedResponse(fa.join(), fb.join(), fc.join(), elapsed, "VIRTUAL");
    }
}
