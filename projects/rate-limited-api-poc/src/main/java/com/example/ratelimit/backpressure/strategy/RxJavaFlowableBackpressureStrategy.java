package com.example.ratelimit.backpressure.strategy;

import com.example.ratelimit.backpressure.BackpressureStrategy;
import com.example.ratelimit.backpressure.WorkItem;
import com.example.ratelimit.client.TrackedExternalApiClient;
import com.example.ratelimit.config.BackpressureProperties;
import com.example.ratelimit.ratelimit.RedisTokenBucketRateLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Flowable + onBackpressureBuffer + Redis Token Bucket.
 *
 * <p>request(1) → token 획득 → 외부 API 호출 → deferredResult.setResult().
 */
@Component
@ConditionalOnProperty(
    name = "app.backpressure.strategy",
    havingValue = "rxjava",
    matchIfMissing = true)
public class RxJavaFlowableBackpressureStrategy implements BackpressureStrategy {

  private static final Logger log = LoggerFactory.getLogger(RxJavaFlowableBackpressureStrategy.class);
  private static final long RATE_LIMIT_WAIT_MS = 30_000;

  private final PublishProcessor<WorkItem> processor = PublishProcessor.create();
  private final Semaphore capacity;
  private final int bufferSize;
  private final RedisTokenBucketRateLimiter rateLimiter;
  private final TrackedExternalApiClient externalApi;

  public RxJavaFlowableBackpressureStrategy(
      BackpressureProperties props,
      RedisTokenBucketRateLimiter rateLimiter,
      TrackedExternalApiClient externalApi) {
    this.bufferSize = props.queueCapacity();
    this.capacity = new Semaphore(bufferSize);
    this.rateLimiter = rateLimiter;
    this.externalApi = externalApi;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void start() {
    processor
        .onBackpressureBuffer(
            bufferSize,
            () -> log.warn("Backpressure buffer overflow"),
            io.reactivex.rxjava3.core.BackpressureOverflowStrategy.DROP_OLDEST)
        .observeOn(Schedulers.single())
        .subscribe(
            this::process,
            e -> log.error("Flowable error", e),
            () -> log.info("Flowable completed"));
    log.info("RxJavaFlowableBackpressureStrategy started");
  }

  @Override
  public boolean offer(WorkItem item) {
    if (!capacity.tryAcquire()) {
      return false;
    }
    processor.onNext(item);
    return true;
  }

  @Override
  public int remainingCapacity() {
    return Math.max(0, capacity.availablePermits());
  }

  private void process(WorkItem item) {
    try {
      if (!rateLimiter.acquireWithWait(RATE_LIMIT_WAIT_MS)) {
        item.deferredResult().setResult(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Rate limit timeout")));
        return;
      }
      JsonNode result = externalApi.call("/delay/0");
      item.deferredResult().setResult(ResponseEntity.ok(result));
    } catch (Exception e) {
      log.error("Process failed for requestId={}", item.requestId(), e);
      item.deferredResult().setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage())));
    } finally {
      capacity.release();
    }
  }
}
