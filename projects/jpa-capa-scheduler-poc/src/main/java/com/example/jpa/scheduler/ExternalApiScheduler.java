package com.example.jpa.scheduler;

import com.example.jpa.dto.ExternalApiResponse;
import com.example.jpa.embeddable.CutOffPeriod;
import com.example.jpa.entity.WarehouseCutoffRequest;
import com.example.jpa.enums.RequestStatus;
import com.example.jpa.repository.WarehouseCutoffRequestRepository;
import com.example.jpa.service.ExternalApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalApiScheduler {

    private final ExternalApiService externalApiService;
    private final WarehouseCutoffRequestRepository requestRepository;

    /**
     * 매 30초마다 외부 API를 호출합니다.
     *
     * fixedDelay: 이전 작업이 끝난 후 30초 후에 다시 실행
     * initialDelay: 애플리케이션 시작 후 10초 후에 첫 실행
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void callExternalApiEvery30Seconds() {
        log.info("======================================");
        log.info("[@Scheduled] 외부 API 호출 시작 - {}", getCurrentTime());
        log.info("======================================");

        // 요청 엔티티 생성
        WarehouseCutoffRequest request = createRequest(1L);

        try {
            List<ExternalApiResponse> posts = externalApiService.fetchPosts();

            // 처음 3개만 로그로 출력
            posts.stream()
                    .limit(3)
                    .forEach(post -> log.info("  - Post ID: {}, Title: {}", post.getId(), post.getTitle()));

            log.info("[@Scheduled] 외부 API 호출 완료 - 총 {} 건", posts.size());

            // 성공 상태로 저장
            request.markSuccess();
            requestRepository.save(request);
            log.info("[@Scheduled] 요청 성공 상태 저장 완료 - Request ID: {}", request.getId());

        } catch (Exception e) {
            log.error("[@Scheduled] 외부 API 호출 중 오류 발생: {}", e.getMessage());
            // 실패 상태로 저장
            request.markFailed(e.getMessage());
            requestRepository.save(request);
            log.info("[@Scheduled] 요청 실패 상태 저장 완료 - Request ID: {}", request.getId());
        }

        log.info("======================================\n");
    }

    /**
     * 매 1분마다 특정 게시글을 조회합니다.
     *
     * cron: 매 1분의 0초에 실행 (예: 10:00:00, 10:01:00, 10:02:00)
     */
    @Scheduled(cron = "0 * * * * *")
    public void callExternalApiEveryMinute() {
        log.info("======================================");
        log.info("[@Scheduled - Cron] 특정 게시글 조회 시작 - {}", getCurrentTime());
        log.info("======================================");

        // 요청 엔티티 생성
        WarehouseCutoffRequest request = createRequest(2L);

        try {
            // ID가 1인 게시글 조회
            ExternalApiResponse post = externalApiService.fetchPostById(1L);

            if (post != null) {
                log.info("  - Post ID: {}", post.getId());
                log.info("  - Title: {}", post.getTitle());
                log.info("  - Body: {}", post.getBody().substring(0, Math.min(50, post.getBody().length())) + "...");
            }

            log.info("[@Scheduled - Cron] 특정 게시글 조회 완료");

            // 성공 상태로 저장
            request.markSuccess();
            requestRepository.save(request);
            log.info("[@Scheduled - Cron] 요청 성공 상태 저장 완료 - Request ID: {}", request.getId());

        } catch (Exception e) {
            log.error("[@Scheduled - Cron] 외부 API 호출 중 오류 발생: {}", e.getMessage());
            // 실패 상태로 저장
            request.markFailed(e.getMessage());
            requestRepository.save(request);
            log.info("[@Scheduled - Cron] 요청 실패 상태 저장 완료 - Request ID: {}", request.getId());
        }

        log.info("======================================\n");
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    /**
     * 요청 엔티티 생성 헬퍼 메서드
     */
    private WarehouseCutoffRequest createRequest(Long warehouseId) {
        WarehouseCutoffRequest request = new WarehouseCutoffRequest();
        try {
            java.lang.reflect.Field field = request.getClass().getDeclaredField("warehouseId");
            field.setAccessible(true);
            field.set(request, warehouseId);

            field = request.getClass().getDeclaredField("cutOffPeriod");
            field.setAccessible(true);
            field.set(request, new CutOffPeriod<>(LocalDateTime.now(), LocalDateTime.now().plusDays(1)));

            field = request.getClass().getDeclaredField("status");
            field.setAccessible(true);
            field.set(request, RequestStatus.SYNCRSLT); // 초기 상태는 성공으로 설정 (후에 실패 시 변경)

            field = request.getClass().getDeclaredField("createdBy");
            field.setAccessible(true);
            field.set(request, 1L);

            field = request.getClass().getDeclaredField("updatedBy");
            field.setAccessible(true);
            field.set(request, 1L);

        } catch (Exception e) {
            log.error("요청 엔티티 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("요청 엔티티 생성 중 오류 발생", e);
        }
        return request;
    }
}
