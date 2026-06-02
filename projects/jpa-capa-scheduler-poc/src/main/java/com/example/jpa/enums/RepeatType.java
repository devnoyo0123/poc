package com.example.jpa.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RepeatType {
    DAILY("매일"),
    WEEKLY("주간");

    private final String description;
}
