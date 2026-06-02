package com.example.apiretry.controller;

import com.example.apiretry.dto.ApiCallRequest;
import com.example.apiretry.dto.ApiCallResponse;
import com.example.apiretry.service.ApiCallService;
import com.example.apiretry.service.ApiCallRetryServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/call")
@RequiredArgsConstructor
@Slf4j
public class ApiCallController {

    private final ApiCallService apiCallService;
    private final ApiCallRetryServiceV2 apiCallRetryServiceV2;

    /**
     * 외부 API를 호출하고 결과를 저장합니다.
     *
     * POST /api/call
     * Body: { "url": "https://api.example.com/data" }
     */
    @PostMapping
    public ResponseEntity<ApiCallResponse> callApi(@RequestBody ApiCallRequest request) throws Exception {
        log.info("Received API call request for URL: {}", request.getUrl());
        ApiCallResponse response = apiCallService.callAndSaveApi(request.getUrl());
        return ResponseEntity.ok(response);
    }

    /**
     * 외부 API를 호출하고 결과를 저장합니다 (버전 2).
     *
     * POST /api/call/v2
     * Body: { "url": "https://api.example.com/data" }
     */
    @PostMapping("/v2")
    public ResponseEntity<ApiCallResponse> callApiV2(@RequestBody ApiCallRequest request) throws Exception {
        log.info("Received API call request for URL: {}", request.getUrl());
        ApiCallResponse response = apiCallRetryServiceV2.callAndSaveApi(request.getUrl());
        return ResponseEntity.ok(response);
    }

    /**
     * GET 방식으로 외부 API를 호출하고 결과를 저장합니다.
     *
     * GET /api/call?url=https://api.example.com/data
     */
    @GetMapping
    public ResponseEntity<ApiCallResponse> callApiWithGet(@RequestParam String url) throws Exception {
        log.info("Received API call request for URL: {}", url);
        ApiCallResponse response = apiCallService.callAndSaveApi(url);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 엔드포인트의 호출 이력을 조회합니다.
     *
     * GET /api/call/history?endpoint=https://api.example.com/data
     */
    @GetMapping("/history")
    public ResponseEntity<List<ApiCallResponse>> getCallHistory(@RequestParam String endpoint) {
        log.info("Fetching call history for endpoint: {}", endpoint);
        List<ApiCallResponse> history = apiCallService.getCallHistory(endpoint);
        return ResponseEntity.ok(history);
    }
}

