package com.example.ratelimit.backpressure.strategy;

import com.example.ratelimit.backpressure.BackpressureQueue;
import com.example.ratelimit.backpressure.BackpressureStrategy;
import com.example.ratelimit.backpressure.WorkItem;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Bounded BlockingQueue + Worker 기반. (기존 구현) */
@Component
@ConditionalOnProperty(name = "app.backpressure.strategy", havingValue = "blocking-queue")
public class BlockingQueueBackpressureStrategy implements BackpressureStrategy {

  private final BackpressureQueue queue;

  public BlockingQueueBackpressureStrategy(BackpressureQueue queue) {
    this.queue = queue;
  }

  @Override
  public boolean offer(WorkItem item) {
    return queue.offer(item);
  }

  @Override
  public int remainingCapacity() {
    return queue.remainingCapacity();
  }
}
