package com.example.ratelimit.backpressure;

import com.example.ratelimit.config.BackpressureProperties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Bounded Queue. blocking-queue 전략일 때만 활성화. */
@Component
@ConditionalOnProperty(name = "app.backpressure.strategy", havingValue = "blocking-queue")
public class BackpressureQueue {

  private final BlockingQueue<WorkItem> queue;

  public BackpressureQueue(BackpressureProperties props) {
    this.queue = new LinkedBlockingQueue<>(props.queueCapacity());
  }

  public boolean offer(WorkItem item) {
    return queue.offer(item);
  }

  public WorkItem take() throws InterruptedException {
    return queue.take();
  }

  public int remainingCapacity() {
    return queue.remainingCapacity();
  }
}
