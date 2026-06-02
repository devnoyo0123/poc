package com.example.jpa.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RequestStatus {
    SYNCRSLT("결과연동완료"),
    ERROCCUR("에러");

    private final String description;
}
