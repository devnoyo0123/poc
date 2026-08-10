package com.poc.asynccontext.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AggregatedResponse {
    private ResponseA a;
    private ResponseB b;
    private ResponseC c;
    private long elapsedMs;
    private String mode;
}
