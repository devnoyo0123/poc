package com.example.jpa.repository;

import com.example.jpa.entity.WarehouseCutoffRequest;
import com.example.jpa.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseCutoffRequestRepository extends JpaRepository<WarehouseCutoffRequest, Long> {

    /**
     * 창고 ID로 요청 목록 조회
     */
    List<WarehouseCutoffRequest> findByWarehouseId(Long warehouseId);

    /**
     * 상태로 요청 목록 조회
     */
    List<WarehouseCutoffRequest> findByStatus(RequestStatus status);

    /**
     * 창고 ID와 상태로 요청 목록 조회
     */
    List<WarehouseCutoffRequest> findByWarehouseIdAndStatus(Long warehouseId, RequestStatus status);

    /**
     * 창고 ID로 요청 목록 조회 (최신순)
     */
    List<WarehouseCutoffRequest> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId);
}
