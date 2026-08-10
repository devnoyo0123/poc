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

@RestController
@RequestMapping("/blocking")
public class BlockingController {

    private final ExternalServiceA serviceA;
    private final ExternalServiceB serviceB;
    private final ExternalServiceC serviceC;

    public BlockingController(ExternalServiceA serviceA,
                              ExternalServiceB serviceB,
                              ExternalServiceC serviceC) {
        this.serviceA = serviceA;
        this.serviceB = serviceB;
        this.serviceC = serviceC;
    }

    @GetMapping
    public AggregatedResponse blocking() {
        long start = System.currentTimeMillis();

        CompletableFuture<ResponseA> fa = serviceA.callAsync();
        CompletableFuture<ResponseB> fb = serviceB.callAsync();
        CompletableFuture<ResponseC> fc = serviceC.callAsync();

        CompletableFuture.allOf(fa, fb, fc).join();

        long elapsed = System.currentTimeMillis() - start;
        return new AggregatedResponse(fa.join(), fb.join(), fc.join(), elapsed, "BLOCKING");
    }
}
