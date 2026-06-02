package com.example.jpa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * JSONPlaceholder API 응답 DTO
 * https://jsonplaceholder.typicode.com/posts
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalApiResponse {
    private Long userId;
    private Long id;
    private String title;
    private String body;
    private Boolean success;  // API 성공 여부 (true/false)

    /**
     * 여러 Post를 담는 래퍼 클래스
     */
    @Data
    @NoArgsConstructor
    public static class PostList {
        private List<ExternalApiResponse> posts;
        private int count;
        private Boolean success;  // API 성공 여부 (true/false)
    }
}
