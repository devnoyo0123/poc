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
import java.util.concurrent.CompletionStage;

/**
 * 반환 타입 = CompletionStage (인터페이스).
 * CompletableFuture는 CompletionStage의 구현체. 즉 /async 와 메커니즘 동일.
 * 차이는 "타입을 인터페이스로 노출" 한 것뿐 — 구현(CompletableFuture/다른 라이브러리)에 안 묶임.
 * Spring은 CompletionStage도 DeferredResult로 어댑트해서 워커 반납 처리.
 */
@RestController
@RequestMapping("/completion-stage")
public class CompletionStageController {

    private final ExternalServiceA serviceA;
    private final ExternalServiceB serviceB;
    private final ExternalServiceC serviceC;

    public CompletionStageController(ExternalServiceA serviceA,
                                     ExternalServiceB serviceB,
                                     ExternalServiceC serviceC) {
        this.serviceA = serviceA;
        this.serviceB = serviceB;
        this.serviceC = serviceC;
    }

    @GetMapping
    public CompletionStage<AggregatedResponse> completionStage() {
        long start = System.currentTimeMillis();

        CompletableFuture<ResponseA> fa = serviceA.callAsync();
        CompletableFuture<ResponseB> fb = serviceB.callAsync();
        CompletableFuture<ResponseC> fc = serviceC.callAsync();

        return CompletableFuture.allOf(fa, fb, fc)
                .thenApply(v -> {
                    long elapsed = System.currentTimeMillis() - start;
                    return new AggregatedResponse(fa.join(), fb.join(), fc.join(), elapsed, "COMPLETION_STAGE");
                });
    }
}