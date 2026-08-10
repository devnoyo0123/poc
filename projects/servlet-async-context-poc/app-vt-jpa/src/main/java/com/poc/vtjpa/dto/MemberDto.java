package com.poc.vtjpa.dto;

import com.poc.vtjpa.entity.Member;

import java.time.Instant;

public record MemberDto(
        Long id,
        String name,
        String email,
        Instant createdAt
) {
    public static MemberDto from(Member m) {
        return new MemberDto(m.getId(), m.getName(), m.getEmail(), m.getCreatedAt());
    }
}
