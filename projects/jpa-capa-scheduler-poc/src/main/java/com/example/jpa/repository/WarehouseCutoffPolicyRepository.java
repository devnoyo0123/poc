package com.example.jpa.repository;

import com.example.jpa.entity.WarehouseCutoffPolicy;
import com.example.jpa.enums.RepeatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseCutoffPolicyRepository extends JpaRepository<WarehouseCutoffPolicy, Long> {

    /**
     * 창고 ID로 정책 목록 조회
     * @Where 애노테이션으로 deleted_at IS NULL 자동 적용
     */
    List<WarehouseCutoffPolicy> findByWarehouseId(Long warehouseId);

    /**
     * 반복 유형으로 정책 목록 조회
     */
    List<WarehouseCutoffPolicy> findByRepeatType(RepeatType repeatType);

    /**
     * 창고 ID와 반복 유형으로 정책 조회
     */
    List<WarehouseCutoffPolicy> findByWarehouseIdAndRepeatType(Long warehouseId, RepeatType repeatType);
}
