package com.example.jpa.service;

import com.example.jpa.dto.CreateRequestRequest;
import com.example.jpa.embeddable.CutOffPeriod;
import com.example.jpa.entity.WarehouseCutoffRequest;
import com.example.jpa.repository.WarehouseCutoffRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseCutoffRequestService {

    private final WarehouseCutoffRequestRepository repository;

    /**
     * 요청 생성
     */
    @Transactional
    public WarehouseCutoffRequest createRequest(CreateRequestRequest request) {
        log.info("요청 생성: warehouseId={}, status={}",
                request.getWarehouseId(), request.getStatus());

        WarehouseCutoffRequest cutoffRequest = new WarehouseCutoffRequest();

        try {
            setField(cutoffRequest, "warehouseId", request.getWarehouseId());
            setField(cutoffRequest, "cutOffPeriod", new CutOffPeriod<>(request.getStartAt(), request.getEndAt()));
            setField(cutoffRequest, "status", request.getStatus());
            setField(cutoffRequest, "errorMessage", request.getErrorMessage());
            setField(cutoffRequest, "createdBy", request.getCreatedBy());
            setField(cutoffRequest, "updatedBy", request.getCreatedBy());

            WarehouseCutoffRequest saved = repository.save(cutoffRequest);
            log.info("요청 생성 완료: id={}", saved.getId());

            return saved;
        } catch (Exception e) {
            log.error("요청 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("요청 생성 중 오류 발생", e);
        }
    }

    /**
     * 모든 요청 조회
     */
    @Transactional(readOnly = true)
    public List<WarehouseCutoffRequest> findAll() {
        return repository.findAll();
    }

    /**
     * ID로 요청 조회
     */
    @Transactional(readOnly = true)
    public WarehouseCutoffRequest findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다: id=" + id));
    }

    /**
     * 창고 ID로 요청 조회
     */
    @Transactional(readOnly = true)
    public List<WarehouseCutoffRequest> findByWarehouseId(Long warehouseId) {
        return repository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId);
    }

    // Reflection helper method
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
