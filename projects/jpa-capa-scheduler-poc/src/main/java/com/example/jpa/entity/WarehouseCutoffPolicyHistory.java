package com.example.jpa.entity;

import com.example.jpa.embeddable.CutOffPeriod;
import com.example.jpa.enums.RepeatType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "warehouse_cutoff_policy_history")
@Where(clause = "deleted_at IS NULL")
@Getter
@NoArgsConstructor
public class WarehouseCutoffPolicyHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_cutoff_policy_id", nullable = false)
    private Long warehouseCutoffPolicyId;

    @Column(nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepeatType repeatType;

    @Embedded
    private CutOffPeriod<LocalTime> cutOffPeriod;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
