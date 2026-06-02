package com.example.apiretry.dto;

import com.example.apiretry.entity.ApiCallAttempt;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiCallResponse {
    private Long id;
    private String endpoint;
    private Integer statusCode;
    private String status;
    private String responseBody;
    private String errorMessage;
    private Integer attemptCount;
    private Boolean isSuccess;
    private LocalDateTime callTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ApiCallAttempt> attempts;
}
