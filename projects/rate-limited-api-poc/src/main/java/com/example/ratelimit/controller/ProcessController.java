package com.example.ratelimit.controller;

import com.example.ratelimit.process.SyncProcessService;
import com.example.ratelimit.verify.ExternalApiCallTracker;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 동기 처리. JSON 요청/응답.
 *
 * <p>/process → SyncProcessService (rate limit + 외부 API) → JSON 응답.
 */
@RestController
@RequestMapping("/api")
public class ProcessController {

  private final SyncProcessService syncProcess;
  private final ExternalApiCallTracker callTracker;

  public ProcessController(SyncProcessService syncProcess, ExternalApiCallTracker callTracker) {
    this.syncProcess = syncProcess;
    this.callTracker = callTracker;
  }

  @GetMapping("/health")
  public Map<String, String> health() {
    return Map.of(
        "status", "UP",
        "queueRemaining", String.valueOf(syncProcess.remainingCapacity()));
  }

  @GetMapping("/stats")
  public Map<String, Object> stats(@RequestParam(defaultValue = "60") int seconds) {
    List<ExternalApiCallTracker.PerSecondCount> perSecond = callTracker.getPerSecondCounts(seconds);
    int maxPerSecond =
        perSecond.isEmpty() ? 0 : perSecond.stream().mapToInt(ExternalApiCallTracker.PerSecondCount::count).max().orElse(0);
    return Map.of(
        "lastSeconds", seconds,
        "perSecond", perSecond,
        "maxCallsPerSecond", maxPerSecond,
        "rateLimitOk", maxPerSecond <= 2);
  }

  @PostMapping(
      value = "/process",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> process(@RequestBody(required = false) ProcessRequest request) {
    String body = request != null ? request.payload() : "";
    return syncProcess.process(body);
  }
}
