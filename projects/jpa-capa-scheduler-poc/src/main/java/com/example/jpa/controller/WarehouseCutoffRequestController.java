package com.example.jpa.controller;

import com.example.jpa.dto.CreateRequestRequest;
import com.example.jpa.entity.WarehouseCutoffRequest;
import com.example.jpa.service.WarehouseCutoffRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class WarehouseCutoffRequestController {

    private final WarehouseCutoffRequestService service;

    /**
     * 요청 생성
     * POST /api/requests
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRequest(@RequestBody CreateRequestRequest request) {
        log.info("POST /api/requests - 요청 생성");

        try {
            WarehouseCutoffRequest cutoffRequest = service.createRequest(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "요청이 생성되었습니다.");
            response.put("data", cutoffRequest);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("요청 생성 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 모든 요청 조회
     * GET /api/requests
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRequests() {
        log.info("GET /api/requests - 모든 요청 조회");

        try {
            List<WarehouseCutoffRequest> requests = service.findAll();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", requests.size());
            response.put("data", requests);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("요청 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * ID로 요청 조회
     * GET /api/requests/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRequestById(@PathVariable Long id) {
        log.info("GET /api/requests/{} - 요청 조회", id);

        try {
            WarehouseCutoffRequest request = service.findById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", request);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("요청 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    /**
     * 창고 ID로 요청 조회
     * GET /api/requests/warehouse/{warehouseId}
     */
    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<Map<String, Object>> getRequestsByWarehouseId(@PathVariable Long warehouseId) {
        log.info("GET /api/requests/warehouse/{} - 창고별 요청 조회", warehouseId);

        try {
            List<WarehouseCutoffRequest> requests = service.findByWarehouseId(warehouseId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("warehouseId", warehouseId);
            response.put("count", requests.size());
            response.put("data", requests);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("요청 조회 실패: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
