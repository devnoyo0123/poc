package com.example.asyncdemo.controller;

import com.example.asyncdemo.dto.UserProfile;
import com.example.asyncdemo.service.UserProfileIntegrationService;
import com.example.asyncdemo.service.UserProfileIntegrationService.ComparisonResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 여러 API 통합 호출 테스트 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
public class IntegrationController {

    private final UserProfileIntegrationService integrationService;

    /**
     * 순차 통합 호출
     * GET /api/integration/sequential/{userId}
     */
    @GetMapping("/sequential/{userId}")
    public ResponseEntity<UserProfile> sequentialIntegration(@PathVariable Long userId) {
        UserProfile result = integrationService.fetchAndSaveSequential(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 병렬 통합 호출
     * GET /api/integration/parallel/{userId}
     */
    @GetMapping("/parallel/{userId}")
    public ResponseEntity<UserProfile> parallelIntegration(@PathVariable Long userId) {
        UserProfile result = integrationService.fetchAndSaveParallel(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 성능 비교
     * GET /api/integration/compare/{userId}
     */
    @GetMapping("/compare/{userId}")
    public ResponseEntity<Map<String, Object>> compareIntegration(@PathVariable Long userId) {
        ComparisonResult comparison = integrationService.compareIntegration(userId);

        return ResponseEntity.ok(Map.of(
                "comparison", comparison,
                "description", Map.of(
                        "apis", "사용자, 주문, 결제, 포인트 (4개 API)",
                        "sequentialTime", "각 API를 순차 호출",
                        "parallelTime", "4개 API를 동시에 호출"
                )
        ));
    }

    /**
     * 에러 추적 병렬 호출
     * 각 API의 성공/실패 상태를 추적
     * GET /api/integration/error-tracking/{userId}
     */
    @GetMapping("/error-tracking/{userId}")
    public ResponseEntity<UserProfile> errorTrackingIntegration(@PathVariable Long userId) {
        UserProfile result = integrationService.fetchAndSaveWithErrorTracking(userId);
        return ResponseEntity.ok(result);
    }
}
