package com.example.jpa.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class OverlappingRange {
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final String pattern; // DAILY, WEEKLY, MANUAL
}
