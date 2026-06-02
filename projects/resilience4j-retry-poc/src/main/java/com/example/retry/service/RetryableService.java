package com.example.retry.service;

import com.example.retry.entity.SampleEntity;
import com.example.retry.repository.SampleRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryableService {

    private final SampleRepository sampleRepository;
    private final AtomicInteger attemptCounter = new AtomicInteger(0);

    /**
     * Retry가 성공하면 @Transactional이 정상적으로 커밋되는지 테스트
     *
     * 시나리오:
     * - 1차 시도: RuntimeException 발생 → retry
     * - 2차 시도: 성공 → DB에 저장되고 커밋
     */
    @Retry(name = "sampleService")
    @Transactional
    public SampleEntity saveWithRetry(String name, int failUntilAttempt) {
        int currentAttempt = attemptCounter.incrementAndGet();
        log.info("=== Attempt #{} - saveWithRetry called with name: {}", currentAttempt, name);

        SampleEntity entity = new SampleEntity(name);
        entity.incrementRetryCount();

        // 지정된 시도 횟수까지는 실패
        if (currentAttempt < failUntilAttempt) {
            log.warn("Attempt #{} - Throwing RuntimeException (will retry)", currentAttempt);
            throw new RuntimeException("Simulated failure on attempt " + currentAttempt);
        }

        log.info("Attempt #{} - Success! Saving entity to DB", currentAttempt);
        SampleEntity saved = sampleRepository.save(entity);
        log.info("Attempt #{} - Entity saved with ID: {}", currentAttempt, saved.getId());

        return saved;
    }

    /**
     * MaxAttempts를 초과하면 마지막 예외가 그대로 던져지는지 테스트
     * 참고: Resilience4j의 fallbackMethod는 Spring Retry와 달리 자동 호출되지 않음
     */
    @Retry(name = "maxRetryService")
    @Transactional
    public SampleEntity saveWithMaxRetry(String name) {
        int currentAttempt = attemptCounter.incrementAndGet();
        log.info("=== Attempt #{} - saveWithMaxRetry called with name: {}", currentAttempt, name);

        SampleEntity entity = new SampleEntity(name);
        entity.incrementRetryCount();
        sampleRepository.save(entity);

        log.warn("Attempt #{} - Throwing RuntimeException (will retry)", currentAttempt);
        throw new RuntimeException("Always fails - simulated failure on attempt " + currentAttempt);
    }

    /**
     * Retry 중에 트랜잭션 롤백이 발생하는지 테스트
     *
     * 시나리오:
     * - 1차 시도: DB 저장 후 RuntimeException 발생 → 롤백 → retry
     * - 2차 시도: DB 저장 후 RuntimeException 발생 → 롤백 → retry
     * - 3차 시도: 성공 → 커밋
     */
    @Retry(name = "sampleService")
    @Transactional
    public SampleEntity saveWithRollbackAndRetry(String name, int failUntilAttempt) {
        int currentAttempt = attemptCounter.incrementAndGet();
        log.info("=== Attempt #{} - saveWithRollbackAndRetry called with name: {}", currentAttempt, name);

        // 항상 DB에 먼저 저장
        SampleEntity entity = new SampleEntity(name + "-attempt-" + currentAttempt);
        entity.incrementRetryCount();
        SampleEntity saved = sampleRepository.save(entity);
        log.info("Attempt #{} - Entity saved with ID: {}", currentAttempt, saved.getId());

        // 지정된 시도 횟수까지는 실패 (저장 후 예외 발생 → 롤백)
        if (currentAttempt < failUntilAttempt) {
            log.warn("Attempt #{} - Throwing exception AFTER save (will rollback and retry)", currentAttempt);
            throw new RuntimeException("Simulated failure after save on attempt " + currentAttempt);
        }

        log.info("Attempt #{} - Success! Transaction will commit", currentAttempt);
        return saved;
    }


    public void resetCounter() {
        attemptCounter.set(0);
        log.info("Counter reset to 0");
    }

    public int getCurrentAttemptCount() {
        return attemptCounter.get();
    }
}
