package com.example.ratelimit.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** /process JSON 요청 본문. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessRequest(String payload) {
  public ProcessRequest {
    payload = payload != null ? payload : "";
  }
}
