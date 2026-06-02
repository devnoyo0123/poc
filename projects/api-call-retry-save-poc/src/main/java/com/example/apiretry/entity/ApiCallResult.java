package com.example.apiretry.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "api_call_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCallResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String endpoint;

    @Column(nullable = false)
    private Integer statusCode;

    @Column(nullable = false)
    private String status;

    @Column(length = 2000)
    private String responseBody;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private Integer attemptCount;

    @Column(nullable = false)
    private Boolean isSuccess;

    @Column(nullable = false)
    private LocalDateTime callTime;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @JsonIgnore
    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApiCallAttempt> attempts;
}
