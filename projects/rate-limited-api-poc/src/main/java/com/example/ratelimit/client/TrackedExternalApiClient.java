package com.example.ratelimit.client;

import com.example.ratelimit.config.ExternalApiProperties;
import com.example.ratelimit.verify.ExternalApiCallTracker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.stereotype.Component;

/** ExternalApiClient 래퍼. 실제 호출 시 Tracker에 기록. simulated=true면 HTTP 대신 메서드 호출처럼 처리. */
@Component
public class TrackedExternalApiClient {

  private final ExternalApiClient delegate;
  private final ExternalApiCallTracker tracker;
  private final boolean simulated;

  public TrackedExternalApiClient(
      ExternalApiClient delegate,
      ExternalApiCallTracker tracker,
      ExternalApiProperties props) {
    this.delegate = delegate;
    this.tracker = tracker;
    this.simulated = props != null && props.simulated();
  }

  public JsonNode call(String path) {
    JsonNode result =
        simulated
            ? minimalProcess()
            : delegate.call(path);
    tracker.record();
    return result;
  }

  /** simulated 모드: HTTP 없이 메서드 호출처럼 처리 후 카운트 */
  private JsonNode minimalProcess() {
    return JsonNodeFactory.instance.objectNode();
  }
}
