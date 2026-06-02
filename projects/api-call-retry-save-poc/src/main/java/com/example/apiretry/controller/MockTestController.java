package com.example.apiretry.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mock")
@Slf4j
public class MockTestController {

    private static int attemptCount = 0;

    /**
     * 성공 응답 반환
     */
    @PostMapping("/success")
    public ResponseEntity<String> mockSuccessPost() {
        log.info("Mock success endpoint called");
        attemptCount = 0; // reset counter
        return ResponseEntity.ok("{\"message\": \"Success\", \"data\": \"test\"}");
    }

    /**
     * 항상 500 에러를 반환 (retry 테스트용)
     */
    @GetMapping("/500")
    public ResponseEntity<String> mock500() {
        attemptCount++;
        log.info("Mock 500 endpoint called - attempt #{}", attemptCount);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"error\": \"Internal Server Error\", \"attempt\": " + attemptCount + "}");
    }

    /**
     * 항상 429 에러를 반환 (rate limit 테스트용)
     */
    @GetMapping("/429")
    public ResponseEntity<String> mock429() {
        log.info("Mock 429 endpoint called");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body("{\"error\": \"Too Many Requests\"}");
    }

    /**
     * 항상 400 에러를 반환 (재시도 안 함)
     */
    @GetMapping("/400")
    public ResponseEntity<String> mock400() {
        log.info("Mock 400 endpoint called");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("{\"error\": \"Bad Request\"}");
    }

    /**
     * 항상 404 에러를 반환 (재시도 안 함)
     */
    @GetMapping("/404")
    public ResponseEntity<String> mock404() {
        log.info("Mock 404 endpoint called");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("{\"error\": \"Not Found\"}");
    }

    /**
     * 성공 응답 반환
     */
    @GetMapping("/success")
    public ResponseEntity<String> mockSuccess() {
        log.info("Mock success endpoint called");
        attemptCount = 0; // reset counter
        return ResponseEntity.ok("{\"message\": \"Success\", \"data\": \"test\"}");
    }

    @GetMapping("/reset")
    public ResponseEntity<String> reset() {
        attemptCount = 0;
        log.info("Mock counter reset");
        return ResponseEntity.ok("{\"message\": \"Counter reset\"}");
    }
}
