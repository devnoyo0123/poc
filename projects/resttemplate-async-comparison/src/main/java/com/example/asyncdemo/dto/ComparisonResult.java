package com.example.asyncdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonResult {
    private String executionType; // SEQUENTIAL or PARALLEL
    private long totalTimeMs;
    private List<UserResponse> results;
    private int apiCallCount;
    private double avgTimePerCallMs;
}
