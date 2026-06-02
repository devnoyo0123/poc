package com.example.jpa.service;

import com.example.jpa.dto.ExternalApiResponse;
import com.example.jpa.exception.ApiCallFailedException;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiService {

    public static final String RETRY_NAME = "externalApi";
    private final RestTemplate restTemplate;
    // 테스트용: 항상 500 에러 반환하는 URL
    private static final String EXTERNAL_API_URL = "http://httpstat.us/500";

    /**
     * API 응답의 success 필드를 검증
     */
    private void validateResponse(Boolean success) {
        if (success == null || !success) {
            throw new ApiCallFailedException("API 호출 실패: success=" + success);
        }
    }

    /**
     * 외부 API를 호출하여 게시글 목록을 가져옵니다.
     * Resilience4j Retry를 적용하여 5xx/429/success=false 시 재시도합니다.
     */
    @Retry(name = RETRY_NAME)
    public List<ExternalApiResponse> fetchPosts() {
        log.info("[{}] 외부 API 호출 시작: {}", getCurrentTime(), EXTERNAL_API_URL);

        ExternalApiResponse[] response = restTemplate.getForObject(EXTERNAL_API_URL, ExternalApiResponse[].class);

        if (response != null) {
            log.info("[{}] 외부 API 호출 성공: {} 건의 게시글 조회", getCurrentTime(), response.length);

            // success 필드 검증 (첫 번째 응답의 success 필드 확인)
            if (response.length > 0 && response[0].getSuccess() != null) {
                validateResponse(response[0].getSuccess());
            }

            return Arrays.asList(response);
        } else {
            log.warn("[{}] 외부 API 호출 결과가 null입니다.", getCurrentTime());
            return List.of();
        }
    }

    /**
     * 특정 ID의 게시글을 가져옵니다.
     * Resilience4j Retry를 적용하여 5xx/429/success=false 시 재시도합니다.
     */
    @Retry(name = RETRY_NAME)
    public ExternalApiResponse fetchPostById(Long id) {
        String url = EXTERNAL_API_URL + "/" + id;
        log.info("[{}] 외부 API 호출 시작: {}", getCurrentTime(), url);

        ExternalApiResponse response = restTemplate.getForObject(url, ExternalApiResponse.class);

        if (response != null) {
            log.info("[{}] 외부 API 호출 성공: ID={}, Title={}", getCurrentTime(), response.getId(), response.getTitle());

            // success 필드 검증
            if (response.getSuccess() != null) {
                validateResponse(response.getSuccess());
            }

            return response;
        } else {
            log.warn("[{}] 외부 API 호출 결과가 null입니다. (ID={})", getCurrentTime(), id);
            return null;
        }
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
