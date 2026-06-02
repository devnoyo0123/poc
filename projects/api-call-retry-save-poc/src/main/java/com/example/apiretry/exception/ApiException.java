package com.example.apiretry.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final String url;
    private final int attemptCount;
    private final boolean alreadySaved; // 이미 DB에 저장되었는지 플래그

    public ApiException(String message, String url, int attemptCount, boolean alreadySaved) {
        super(message);
        this.url = url;
        this.attemptCount = attemptCount;
        this.alreadySaved = alreadySaved;
    }

    // 이전 코드와 호환성을 위한 생성자 (기본적으로 alreadySaved = false)
    public ApiException(String message, String url, int attemptCount) {
        this(message, url, attemptCount, false);
    }
}
