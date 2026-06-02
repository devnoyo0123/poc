package com.example.ratelimit.backpressure;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

public record WorkItem(DeferredResult<ResponseEntity<?>> deferredResult, String requestId, String payload) {}
