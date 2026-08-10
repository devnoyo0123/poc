package com.poc.vtjpa.dto;

public record AggregatedResult(
        MemberDto member,
        ProductDto product,
        long elapsedMs,
        String mode
) {
}
