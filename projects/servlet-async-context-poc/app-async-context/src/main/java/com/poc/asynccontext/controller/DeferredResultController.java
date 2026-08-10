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
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;

/**
 * 반환 타입 = DeferredResult.
 * 메커니즘: 컨트롤러는 "빈 DeferredResult"를 즉시 반환 → Spring이 startAsync() → 워커 반납.
 *          결과는 "내가 고른 아무 스레드"에서 deferred.setResult(...)로 채움.
 * Callable과 차이: Callable은 Spring이 스레드를 잡아 실행해줌(수동적).
 *                  DeferredResult는 완성을 "내가 직접" 책임짐(능동적). 외부 콜백/이벤트/메시지큐 응답 등에 적합.
 */
@RestController
@RequestMapping("/deferred")
public class DeferredResultController {

    private final ExternalServiceA serviceA;
    private final ExternalServiceB serviceB;
    private final ExternalServiceC serviceC;

    public DeferredResultController(ExternalServiceA serviceA,
                                    ExternalServiceB serviceB,
                                    ExternalServiceC serviceC) {
        this.serviceA = serviceA;
        this.serviceB = serviceB;
        this.serviceC = serviceC;
    }

    @GetMapping
    public DeferredResult<AggregatedResponse> deferred() {
        long start = System.currentTimeMillis();
        DeferredResult<AggregatedResponse> output = new DeferredResult<>();

        CompletableFuture<ResponseA> fa = serviceA.callAsync();
        CompletableFuture<ResponseB> fb = serviceB.callAsync();
        CompletableFuture<ResponseC> fc = serviceC.callAsync();

        // 일 끝나면(ext풀 스레드에서) 직접 setResult → 그 스레드가 응답 트리거
        CompletableFuture.allOf(fa, fb, fc).whenComplete((v, ex) -> {
            if (ex != null) {
                output.setErrorResult(ex);
                return;
            }
            long elapsed = System.currentTimeMillis() - start;
            output.setResult(new AggregatedResponse(fa.join(), fb.join(), fc.join(), elapsed, "DEFERRED"));
        });

        return output;  // 즉시 반환 → 워커 반납. 위 콜백이 나중에 채움.
    }
}