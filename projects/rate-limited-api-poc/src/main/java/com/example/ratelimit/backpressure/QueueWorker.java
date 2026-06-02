package com.example.ratelimit.backpressure;

import com.example.ratelimit.client.TrackedExternalApiClient;
import com.example.ratelimit.ratelimit.RedisTokenBucketRateLimiter;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** blocking-queue 전략일 때만 활성화. */
@Component
@ConditionalOnProperty(name = "app.backpressure.strategy", havingValue = "blocking-queue")
public class QueueWorker {

  private static final Logger log = LoggerFactory.getLogger(QueueWorker.class);
  private static final long RATE_LIMIT_WAIT_MS = 30_000;

  private final BackpressureQueue queue;
  private final RedisTokenBucketRateLimiter rateLimiter;
  private final TrackedExternalApiClient externalApi;
  private volatile boolean running = true;

  public QueueWorker(
      BackpressureQueue queue,
      RedisTokenBucketRateLimiter rateLimiter,
      TrackedExternalApiClient externalApi) {
    this.queue = queue;
    this.rateLimiter = rateLimiter;
    this.externalApi = externalApi;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void start() {
    Thread worker = new Thread(this::run, "backpressure-worker");
    worker.setDaemon(false);
    worker.start();
    log.info("QueueWorker started");
  }

  private void run() {
    while (running) {
      try {
        WorkItem item = queue.take();
        process(item);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Worker interrupted");
        break;
      }
    }
  }

  private void process(WorkItem item) {
    try {
      if (!rateLimiter.acquireWithWait(RATE_LIMIT_WAIT_MS)) {
        item.deferredResult().setResult(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Rate limit wait timeout")));
        return;
      }
      JsonNode result = externalApi.call("/delay/0");
      item.deferredResult().setResult(ResponseEntity.ok(result));
    } catch (Exception e) {
      log.error("Process failed for requestId={}", item.requestId(), e);
      item.deferredResult().setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", e.getMessage())));
    }
  }
}
