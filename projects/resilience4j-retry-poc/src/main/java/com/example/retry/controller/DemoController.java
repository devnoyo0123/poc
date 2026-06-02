package com.example.retry.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo API Server - 다양한 HTTP 상태 코드를 반환하여 Retry 동작 테스트
 */
@RestController
@RequestMapping("/api/demo")
@Slf4j
public class DemoController {

    /**
     * 200 OK - 정상 응답
     */
    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> success(@RequestParam(defaultValue = "success") String message) {
        log.info("Demo API: success endpoint called");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 400 Bad Request - 클라이언트 에러 (retry 해도 해결 안됨)
     */
    @GetMapping("/bad-request")
    public ResponseEntity<Map<String, Object>> badRequest() {
        log.warn("Demo API: bad-request endpoint called - returning 400");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", 400);
        response.put("message", "Bad Request - Invalid parameters");
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 404 Not Found - 클라이언트 에러 (retry 해도 해결 안됨)
     */
    @GetMapping("/not-found")
    public ResponseEntity<Map<String, Object>> notFound(@RequestParam String id) {
        log.warn("Demo API: not-found endpoint called with id={} - returning 404", id);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", 404);
        response.put("message", "Resource not found with id: " + id);
        return ResponseEntity.notFound().build();
    }

    /**
     * 500 Internal Server Error - 서버 에러 (retry로 해결 가능)
     */
    @GetMapping("/server-error")
    public ResponseEntity<Map<String, Object>> serverError() {
        log.error("Demo API: server-error endpoint called - returning 500");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", 500);
        response.put("message", "Internal Server Error - Temporary failure");
        return ResponseEntity.internalServerError().body(response);
    }

    /**
     * 503 Service Unavailable - 서버 에러 (retry로 해결 가능)
     */
    @GetMapping("/service-unavailable")
    public ResponseEntity<Map<String, Object>> serviceUnavailable() {
        log.warn("Demo API: service-unavailable endpoint called - returning 503");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", 503);
        response.put("message", "Service Unavailable - Please retry later");
        return ResponseEntity.status(503).body(response);
    }

    /**
     * 요청 카운트를 반환하는 endpoint (retry 횟수 확인용)
     */
    private static int requestCount = 0;

    @GetMapping("/request-count")
    public ResponseEntity<Map<String, Object>> getRequestCount() {
        requestCount++;
        log.info("Demo API: request-count endpoint called - count={}", requestCount);
        Map<String, Object> response = new HashMap<>();
        response.put("count", requestCount);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-count")
    public ResponseEntity<Map<String, String>> resetCount() {
        requestCount = 0;
        log.info("Demo API: request-count reset");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Count reset successfully");
        return ResponseEntity.ok(response);
    }
}
