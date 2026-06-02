package com.example.ratelimit.backpressure;

/**
 * Backpressure 처리 전략.
 *
 * <p>offer() 실패 시 업스트림에 503 등으로 압력 전달.
 */
public interface BackpressureStrategy {

  /** WorkItem 적재 시도. 공간 있으면 true, 없으면 false (backpressure) */
  boolean offer(WorkItem item);

  /** 남은 수용 가능 수 (헬스체크 등용) */
  int remainingCapacity();
}
