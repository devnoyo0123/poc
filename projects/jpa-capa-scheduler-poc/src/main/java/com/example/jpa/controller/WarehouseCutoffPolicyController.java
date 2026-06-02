package com.example.jpa.controller;

import com.example.jpa.dto.CreatePolicyRequest;
import com.example.jpa.entity.WarehouseCutoffPolicy;
import com.example.jpa.service.WarehouseCutoffPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class WarehouseCutoffPolicyController {

    private final WarehouseCutoffPolicyService service;

    /**
     * 정책 생성
     * POST /api/policies
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPolicy(@RequestBody CreatePolicyRequest request) {
        log.info("POST /api/policies - 정책 생성 요청");

        try {
            WarehouseCutoffPolicy policy = service.createPolicy(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "정책이 생성되었습니다.");
            response.put("data", policy);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("정책 생성 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 모든 정책 조회
     * GET /api/policies
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPolicies() {
        log.info("GET /api/policies - 모든 정책 조회");

        try {
            List<WarehouseCutoffPolicy> policies = service.findAll();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", policies.size());
            response.put("data", policies);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("정책 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * ID로 정책 조회
     * GET /api/policies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPolicyById(@PathVariable Long id) {
        log.info("GET /api/policies/{} - 정책 조회", id);

        try {
            WarehouseCutoffPolicy policy = service.findById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", policy);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("정책 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    /**
     * 창고 ID로 정책 조회
     * GET /api/policies/warehouse/{warehouseId}
     */
    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<Map<String, Object>> getPoliciesByWarehouseId(@PathVariable Long warehouseId) {
        log.info("GET /api/policies/warehouse/{} - 창고별 정책 조회", warehouseId);

        try {
            List<WarehouseCutoffPolicy> policies = service.findByWarehouseId(warehouseId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouseId", warehouseId);
            response.put("count", policies.size());
            response.put("data", policies);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("정책 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 정책 삭제 (Soft Delete)
     * DELETE /api/policies/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePolicy(@PathVariable Long id) {
        log.info("DELETE /api/policies/{} - 정책 삭제", id);

        try {
            service.deletePolicy(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "정책이 삭제되었습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("정책 삭제 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
