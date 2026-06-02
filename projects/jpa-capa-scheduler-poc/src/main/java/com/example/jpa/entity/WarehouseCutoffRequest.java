package com.example.jpa.entity;

import com.example.jpa.embeddable.CutOffPeriod;
import com.example.jpa.enums.RequestStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_cutoff_request")
@Getter
@NoArgsConstructor
public class WarehouseCutoffRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long warehouseId;

    @Embedded
    private CutOffPeriod<LocalDateTime> cutOffPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 요청을 성공 상태로 표시
     */
    public void markSuccess() {
        this.status = RequestStatus.SYNCRSLT;
        this.errorMessage = null;
    }

    /**
     * 요청을 실패 상태로 표시
     */
    public void markFailed(String errorMessage) {
        this.status = RequestStatus.ERROCCUR;
        this.errorMessage = errorMessage;
    }
}
