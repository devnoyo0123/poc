package com.example.jpa.controller;

import com.example.jpa.dto.ExternalApiResponse;
import com.example.jpa.service.ExternalApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final ExternalApiService externalApiService;

    /**
     * 외부 API를 호출하여 모든 게시글을 가져옵니다.
     *
     * GET http://localhost:8080/api/demo/posts
     */
    @GetMapping("/posts")
    public ResponseEntity<Map<String, Object>> getAllPosts() {
        log.info("[DemoController] GET /api/demo/posts 호출 - {}", getCurrentTime());

        try {
            List<ExternalApiResponse> posts = externalApiService.fetchPosts();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", getCurrentTime());
            response.put("count", posts.size());
            response.put("data", posts);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[DemoController] 오류 발생: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("timestamp", getCurrentTime());
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 특정 ID의 게시글을 가져옵니다.
     *
     * GET http://localhost:8080/api/demo/posts/1
     */
    @GetMapping("/posts/{id}")
    public ResponseEntity<Map<String, Object>> getPostById(@PathVariable Long id) {
        log.info("[DemoController] GET /api/demo/posts/{} 호출 - {}", id, getCurrentTime());

        try {
            ExternalApiResponse post = externalApiService.fetchPostById(id);

            if (post == null) {
                Map<String, Object> notFoundResponse = new HashMap<>();
                notFoundResponse.put("success", false);
                notFoundResponse.put("timestamp", getCurrentTime());
                notFoundResponse.put("error", "게시글을 찾을 수 없습니다. (ID: " + id + ")");

                return ResponseEntity.status(404).body(notFoundResponse);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", getCurrentTime());
            response.put("data", post);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[DemoController] 오류 발생: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("timestamp", getCurrentTime());
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 수동으로 외부 API를 트리거합니다.
     *
     * POST http://localhost:8080/api/demo/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerExternalApiCall() {
        log.info("[DemoController] POST /api/demo/trigger 호출 - {}", getCurrentTime());

        try {
            List<ExternalApiResponse> posts = externalApiService.fetchPosts();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("timestamp", getCurrentTime());
            response.put("message", "외부 API 호출이 성공적으로 실행되었습니다.");
            response.put("count", posts.size());
            response.put("preview", posts.stream().limit(5).toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[DemoController] 오류 발생: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("timestamp", getCurrentTime());
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 헬스체크 엔드포인트
     *
     * GET http://localhost:8080/api/demo/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", getCurrentTime());
        response.put("message", "Demo Controller is running");

        return ResponseEntity.ok(response);
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
