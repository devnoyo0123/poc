package com.example.ratelimit.verify;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 실제 외부 API 호출 시각 기록. Rate limit 검증용.
 *
 * <p>로그: [RATE_LIMIT_VERIFY] external_api_call epoch_ms=...
 * <p>GET /api/stats: 초당 호출 수 요약
 */
@Component
public class ExternalApiCallTracker {

  private static final Logger log = LoggerFactory.getLogger("RATE_LIMIT_VERIFY");
  private static final int MAX_RECORDS = 10_000;

  private final List<Long> timestamps = new CopyOnWriteArrayList<>();

  public void record() {
    long now = Instant.now().toEpochMilli();
    timestamps.add(now);
    log.info("external_api_call epoch_ms={}", now);
    pruneIfNeeded();
  }

  private void pruneIfNeeded() {
    if (timestamps.size() > MAX_RECORDS) {
      long cutoff = System.currentTimeMillis() - 120_000; // 2분 이전 삭제
      timestamps.removeIf(ts -> ts < cutoff);
    }
  }

  /** 최근 lastSeconds초 구간의 초당 호출 수 (초 단위 버킷 → 건수) */
  public List<PerSecondCount> getPerSecondCounts(int lastSeconds) {
    long cutoff = System.currentTimeMillis() - (lastSeconds * 1000L);
    List<Long> recent =
        new ArrayList<>(timestamps).stream().filter(ts -> ts >= cutoff).sorted().toList();
    if (recent.isEmpty()) {
      return List.of();
    }
    return recent.stream()
        .collect(Collectors.groupingBy(ts -> ts / 1000))
        .entrySet()
        .stream()
        .sorted(Comparator.comparing(e -> e.getKey()))
        .map(e -> new PerSecondCount(e.getKey(), e.getValue().size()))
        .toList();
  }

  public record PerSecondCount(long secondEpoch, int count) {}
}
