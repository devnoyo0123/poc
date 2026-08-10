package com.poc.vtjpa.dto;

import com.poc.vtjpa.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductDto(
        Long id,
        String name,
        BigDecimal price,
        Instant createdAt
) {
    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getPrice(), p.getCreatedAt());
    }
}
