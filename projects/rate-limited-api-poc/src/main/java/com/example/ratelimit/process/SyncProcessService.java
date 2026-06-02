package com.example.ratelimit.process;

import com.example.ratelimit.client.TrackedExternalApiClient;
import com.example.ratelimit.config.BackpressureProperties;
import com.example.ratelimit.ratelimit.RedisTokenBucketRateLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/** Rate limit + 외부 API를 동기 호출 후 결과 반환. */
@Service
public class SyncProcessService {

  private static final long RATE_LIMIT_WAIT_MS = 30_000;

  private final Semaphore concurrency;
  private final RedisTokenBucketRateLimiter rateLimiter;
  private final TrackedExternalApiClient externalApi;

  public SyncProcessService(
      BackpressureProperties props,
      RedisTokenBucketRateLimiter rateLimiter,
      TrackedExternalApiClient externalApi) {
    this.concurrency = new Semaphore(props.queueCapacity());
    this.rateLimiter = rateLimiter;
    this.externalApi = externalApi;
  }

  public ResponseEntity<?> process(String payload) {
    if (!concurrency.tryAcquire()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("error", "Service overloaded", "code", "BACKPRESSURE"));
    }
    try {
      if (!rateLimiter.acquireWithWait(RATE_LIMIT_WAIT_MS)) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Rate limit timeout"));
      }
      JsonNode result = externalApi.call("/delay/0");
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage()));
    } finally {
      concurrency.release();
    }
  }

  public int remainingCapacity() {
    return concurrency.availablePermits();
  }
}
