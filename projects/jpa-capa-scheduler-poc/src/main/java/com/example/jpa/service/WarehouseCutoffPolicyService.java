package com.example.jpa.service;

import com.example.jpa.dto.CreatePolicyRequest;
import com.example.jpa.embeddable.CutOffPeriod;
import com.example.jpa.entity.WarehouseCutoffPolicy;
import com.example.jpa.repository.WarehouseCutoffPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseCutoffPolicyService {

    private final WarehouseCutoffPolicyRepository repository;

    /**
     * 정책 생성
     */
    @Transactional
    public WarehouseCutoffPolicy createPolicy(CreatePolicyRequest request) {
        log.info("정책 생성 요청: warehouseId={}, repeatType={}",
                request.getWarehouseId(), request.getRepeatType());

        WarehouseCutoffPolicy policy = new WarehouseCutoffPolicy();

        try {
            // Reflection을 사용하여 필드 설정 (실제로는 Builder 패턴 사용 권장)
            setField(policy, "warehouseId", request.getWarehouseId());
            setField(policy, "repeatType", request.getRepeatType());
            setField(policy, "cutOffPeriod", new CutOffPeriod<>(request.getStartAt(), request.getEndAt()));
            setField(policy, "createdBy", request.getCreatedBy());
            setField(policy, "updatedBy", request.getCreatedBy());

            WarehouseCutoffPolicy saved = repository.save(policy);

            log.info("정책 생성 완료: id={}", saved.getId());

            return saved;
        } catch (Exception e) {
            log.error("정책 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("정책 생성 중 오류 발생", e);
        }
    }

    /**
     * 모든 정책 조회
     */
    @Transactional(readOnly = true)
    public List<WarehouseCutoffPolicy> findAll() {
        return repository.findAll();
    }

    /**
     * ID로 정책 조회
     */
    @Transactional(readOnly = true)
    public WarehouseCutoffPolicy findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("정책을 찾을 수 없습니다: id=" + id));
    }

    /**
     * 창고 ID로 정책 조회
     */
    @Transactional(readOnly = true)
    public List<WarehouseCutoffPolicy> findByWarehouseId(Long warehouseId) {
        return repository.findByWarehouseId(warehouseId);
    }

    /**
     * 정책 삭제 (Soft Delete)
     */
    @Transactional
    public void deletePolicy(Long id) {
        WarehouseCutoffPolicy policy = findById(id);

        try {
            setField(policy, "deletedAt", LocalDateTime.now());
            repository.save(policy);
            log.info("정책 삭제 완료: id={}", id);
        } catch (Exception e) {
            log.error("정책 삭제 실패: {}", e.getMessage(), e);
            throw new RuntimeException("정책 삭제 중 오류 발생", e);
        }
    }

    // Reflection helper method
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
