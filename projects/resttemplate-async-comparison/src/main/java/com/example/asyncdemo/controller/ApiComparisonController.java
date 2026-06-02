package com.example.asyncdemo.controller;

import com.example.asyncdemo.dto.ComparisonResult;
import com.example.asyncdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiComparisonController {

    private final UserService userService;

    /**
     * 순차 API 호출 테스트
     * GET /api/sequential?userIds=1,2,3,4,5
     */
    @GetMapping("/sequential")
    public ResponseEntity<ComparisonResult> sequentialCall(
            @RequestParam String userIds) {
        List<Long> ids = parseUserIds(userIds);
        ComparisonResult result = userService.fetchUsersSequential(ids);
        return ResponseEntity.ok(result);
    }

    /**
     * 병렬 API 호출 테스트 (CompletableFuture)
     * GET /api/parallel?userIds=1,2,3,4,5
     */
    @GetMapping("/parallel")
    public ResponseEntity<ComparisonResult> parallelCall(
            @RequestParam String userIds) {
        List<Long> ids = parseUserIds(userIds);
        ComparisonResult result = userService.fetchUsersParallel(ids);
        return ResponseEntity.ok(result);
    }

    /**
     * 순차 vs 병렬 성능 비교
     * GET /api/compare?userIds=1,2,3,4,5
     */
    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareCall(
            @RequestParam String userIds) {
        List<Long> ids = parseUserIds(userIds);

        log.info("========== 성능 비교 시작 ==========");
        log.info("API 호출 개수: {}", ids.size());

        // 순차 호출
        ComparisonResult sequentialResult = userService.fetchUsersSequential(ids);

        // 병렬 호출
        ComparisonResult parallelResult = userService.fetchUsersParallel(ids);

        // 개선율 계산
        double improvementRate = ((double) (sequentialResult.getTotalTimeMs() - parallelResult.getTotalTimeMs())
                / sequentialResult.getTotalTimeMs()) * 100;

        Map<String, Object> response = new HashMap<>();
        response.put("sequential", sequentialResult);
        response.put("parallel", parallelResult);
        response.put("comparison", Map.of(
                "sequentialTimeMs", sequentialResult.getTotalTimeMs(),
                "parallelTimeMs", parallelResult.getTotalTimeMs(),
                "savedTimeMs", sequentialResult.getTotalTimeMs() - parallelResult.getTotalTimeMs(),
                "improvementRate", String.format("%.2f%%", improvementRate),
                "apiCallCount", ids.size()
        ));

        log.info("========== 성능 비교 완료 ==========");
        log.info("순차: {}ms, 병렬: {}ms, 개선율: {}%",
                sequentialResult.getTotalTimeMs(),
                parallelResult.getTotalTimeMs(),
                String.format("%.2f%%", improvementRate));

        return ResponseEntity.ok(response);
    }

    private List<Long> parseUserIds(String userIds) {
        return Arrays.stream(userIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}
