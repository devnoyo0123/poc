package com.example.jpa.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class DuplicateCheckResult {
    private final boolean duplicate;
    private final List<OverlappingRange> overlappingRanges;
}
