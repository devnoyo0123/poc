package com.poc.asynccontext.service;

import com.poc.asynccontext.dto.ResponseC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class ExternalServiceC {

    private final Executor externalCallExecutor;
    private final long delayMs;

    public ExternalServiceC(@Qualifier("executorC") Executor externalCallExecutor,
                            @Value("${external-api.delay-ms}") long delayMs) {
        this.externalCallExecutor = externalCallExecutor;
        this.delayMs = delayMs;
    }

    public CompletableFuture<ResponseC> callAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            return new ResponseC("payment-info from C @ " + System.currentTimeMillis());
        }, externalCallExecutor);
    }
}
