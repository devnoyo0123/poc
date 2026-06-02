package com.example.retry;

import com.example.retry.entity.SampleEntity;
import com.example.retry.repository.SampleRepository;
import com.example.retry.service.RetryableService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Slf4j
class RetryTransactionalTest {

    @Autowired
    private RetryableService retryableService;

    @Autowired
    private SampleRepository sampleRepository;

    @BeforeEach
    void setUp() {
        sampleRepository.deleteAll();
        retryableService.resetCounter();
        log.info("\n\n========================================");
        log.info("Test setup complete - DB cleared, counter reset");
        log.info("========================================\n");
    }

    @Test
    @DisplayName("Retry 성공 시 @Transactional이 정상적으로 커밋되는지 테스트")
    void testRetrySuccessWithTransactionalCommit() {
        // given
        String name = "test-retry-success";
        int failUntilAttempt = 2; // 2번째 시도에서 성공

        log.info("\n\n========================================");
        log.info("TEST: Retry Success + Transactional Commit");
        log.info("Expected: 1st attempt fails → 2nd attempt succeeds → Entity saved");
        log.info("========================================\n");

        // when
        SampleEntity result = retryableService.saveWithRetry(name, failUntilAttempt);

        // then
        log.info("\n\n========================================");
        log.info("VERIFICATION:");
        log.info("- Result ID: {}", result.getId());
        log.info("- Attempt count: {}", retryableService.getCurrentAttemptCount());
        log.info("- DB count: {}", sampleRepository.count());
        log.info("========================================\n");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo(name);
        assertThat(retryableService.getCurrentAttemptCount()).isEqualTo(2); // 2번 시도

        // DB에 실제로 저장되었는지 확인 (트랜잭션 커밋 확인)
        assertThat(sampleRepository.count()).isEqualTo(1);
        assertThat(sampleRepository.findById(result.getId())).isPresent();
    }

    @Test
    @DisplayName("MaxAttempts 초과 시 마지막 예외가 그대로 던져지는지 테스트")
    void testMaxAttemptsExceededThrowsException() {
        // given
        String name = "test-max-retry";

        log.info("\n\n========================================");
        log.info("TEST: MaxAttempts Exceeded → Exception");
        log.info("Expected: All 5 attempts fail → RuntimeException thrown (last exception)");
        log.info("========================================\n");

        // when & then
        assertThatThrownBy(() -> retryableService.saveWithMaxRetry(name))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Always fails - simulated failure on attempt 5");

        log.info("\n\n========================================");
        log.info("VERIFICATION:");
        log.info("- Attempt count: {}", retryableService.getCurrentAttemptCount());
        log.info("- DB count: {}", sampleRepository.count());
        log.info("========================================\n");

        assertThat(retryableService.getCurrentAttemptCount()).isEqualTo(5); // 5번 시도

        // 모든 시도가 실패했으므로 DB에는 아무것도 저장되지 않아야 함 (트랜잭션 롤백)
        assertThat(sampleRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Retry 중 트랜잭션 롤백 후 최종 성공 시 커밋되는지 테스트")
    void testRetryWithRollbackAndFinalCommit() {
        // given
        String name = "test-rollback-retry";
        int failUntilAttempt = 3; // 3번째 시도에서 성공

        log.info("\n\n========================================");
        log.info("TEST: Retry with Rollback → Final Commit");
        log.info("Expected: 1st save + fail → rollback → 2nd save + fail → rollback → 3rd save + success → commit");
        log.info("========================================\n");

        // when
        SampleEntity result = retryableService.saveWithRollbackAndRetry(name, failUntilAttempt);

        // then
        log.info("\n\n========================================");
        log.info("VERIFICATION:");
        log.info("- Result ID: {}", result.getId());
        log.info("- Result Name: {}", result.getName());
        log.info("- Attempt count: {}", retryableService.getCurrentAttemptCount());
        log.info("- DB count: {}", sampleRepository.count());
        log.info("========================================\n");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).contains(name);
        assertThat(retryableService.getCurrentAttemptCount()).isEqualTo(3); // 3번 시도

        // 첫 2번의 시도는 롤백되고, 마지막 시도만 커밋되어야 함
        assertThat(sampleRepository.count()).isEqualTo(1);

        // 저장된 엔티티는 3번째 시도의 것이어야 함
        SampleEntity saved = sampleRepository.findById(result.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo(name + "-attempt-3");
    }

    @Test
    @DisplayName("Retry 없이 첫 시도에서 성공하는 경우")
    void testSuccessOnFirstAttempt() {
        // given
        String name = "test-first-success";
        int failUntilAttempt = 1; // 첫 시도에서 성공

        log.info("\n\n========================================");
        log.info("TEST: Success on First Attempt");
        log.info("Expected: 1st attempt succeeds immediately");
        log.info("========================================\n");

        // when
        SampleEntity result = retryableService.saveWithRetry(name, failUntilAttempt);

        // then
        log.info("\n\n========================================");
        log.info("VERIFICATION:");
        log.info("- Result ID: {}", result.getId());
        log.info("- Attempt count: {}", retryableService.getCurrentAttemptCount());
        log.info("- DB count: {}", sampleRepository.count());
        log.info("========================================\n");

        assertThat(result).isNotNull();
        assertThat(retryableService.getCurrentAttemptCount()).isEqualTo(1); // 1번만 시도
        assertThat(sampleRepository.count()).isEqualTo(1);
    }
}
