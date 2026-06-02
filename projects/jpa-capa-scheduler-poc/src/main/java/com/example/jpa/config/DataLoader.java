package com.example.jpa.config;

import com.example.jpa.embeddable.CutOffPeriod;
import com.example.jpa.entity.WarehouseCutoffPolicy;
import com.example.jpa.entity.WarehouseCutoffRequest;
import com.example.jpa.enums.RepeatType;
import com.example.jpa.enums.RequestStatus;
import com.example.jpa.repository.WarehouseCutoffPolicyRepository;
import com.example.jpa.repository.WarehouseCutoffRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 애플리케이션 시작 시 샘플 데이터를 자동으로 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final WarehouseCutoffPolicyRepository policyRepository;
    private final WarehouseCutoffRequestRepository requestRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("=".repeat(60));
        log.info("샘플 데이터 생성 시작...");
        log.info("=".repeat(60));

        createSamplePolicies();
        createSampleRequests();

        log.info("=".repeat(60));
        log.info("샘플 데이터 생성 완료!");
        log.info("=".repeat(60));
    }

    private void createSamplePolicies() throws Exception {
        // Policy 1: 창고 1, DAILY, 14:00-18:00
        WarehouseCutoffPolicy policy1 = new WarehouseCutoffPolicy();
        setField(policy1, "warehouseId", 1L);
        setField(policy1, "repeatType", RepeatType.DAILY);
        setField(policy1, "cutOffPeriod", new CutOffPeriod<>(
                LocalTime.of(14, 0),
                LocalTime.of(18, 0)
        ));
        setField(policy1, "createdBy", 100L);
        setField(policy1, "updatedBy", 100L);
        policyRepository.save(policy1);
        log.info("✓ 정책 생성: 창고 ID={}, 반복={}, 시간={}-{}",
                1L, RepeatType.DAILY, "14:00", "18:00");

        // Policy 2: 창고 1, WEEKLY, 09:00-12:00
        WarehouseCutoffPolicy policy2 = new WarehouseCutoffPolicy();
        setField(policy2, "warehouseId", 1L);
        setField(policy2, "repeatType", RepeatType.WEEKLY);
        setField(policy2, "cutOffPeriod", new CutOffPeriod<>(
                LocalTime.of(9, 0),
                LocalTime.of(12, 0)
        ));
        setField(policy2, "createdBy", 100L);
        setField(policy2, "updatedBy", 100L);
        policyRepository.save(policy2);
        log.info("✓ 정책 생성: 창고 ID={}, 반복={}, 시간={}-{}",
                1L, RepeatType.WEEKLY, "09:00", "12:00");

        // Policy 3: 창고 2, DAILY, 10:00-16:00
        WarehouseCutoffPolicy policy3 = new WarehouseCutoffPolicy();
        setField(policy3, "warehouseId", 2L);
        setField(policy3, "repeatType", RepeatType.DAILY);
        setField(policy3, "cutOffPeriod", new CutOffPeriod<>(
                LocalTime.of(10, 0),
                LocalTime.of(16, 0)
        ));
        setField(policy3, "createdBy", 200L);
        setField(policy3, "updatedBy", 200L);
        policyRepository.save(policy3);
        log.info("✓ 정책 생성: 창고 ID={}, 반복={}, 시간={}-{}",
                2L, RepeatType.DAILY, "10:00", "16:00");
    }

    private void createSampleRequests() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        // Request 1: 창고 1, 성공
        WarehouseCutoffRequest request1 = new WarehouseCutoffRequest();
        setField(request1, "warehouseId", 1L);
        setField(request1, "cutOffPeriod", new CutOffPeriod<>(
                now.minusDays(1).withHour(14).withMinute(0),
                now.minusDays(1).withHour(18).withMinute(0)
        ));
        setField(request1, "status", RequestStatus.SYNCRSLT);
        setField(request1, "createdBy", 100L);
        setField(request1, "updatedBy", 100L);
        requestRepository.save(request1);
        log.info("✓ 요청 생성: 창고 ID={}, 상태={}", 1L, RequestStatus.SYNCRSLT);

        // Request 2: 창고 1, 에러
        WarehouseCutoffRequest request2 = new WarehouseCutoffRequest();
        setField(request2, "warehouseId", 1L);
        setField(request2, "cutOffPeriod", new CutOffPeriod<>(
                now.minusHours(2).withMinute(0),
                now.minusHours(1).withMinute(0)
        ));
        setField(request2, "status", RequestStatus.ERROCCUR);
        setField(request2, "errorMessage", "네트워크 타임아웃");
        setField(request2, "createdBy", 100L);
        setField(request2, "updatedBy", 100L);
        requestRepository.save(request2);
        log.info("✓ 요청 생성: 창고 ID={}, 상태={}, 에러={}", 1L, RequestStatus.ERROCCUR, "네트워크 타임아웃");

        // Request 3: 창고 2, 성공
        WarehouseCutoffRequest request3 = new WarehouseCutoffRequest();
        setField(request3, "warehouseId", 2L);
        setField(request3, "cutOffPeriod", new CutOffPeriod<>(
                now.withHour(10).withMinute(0),
                now.withHour(16).withMinute(0)
        ));
        setField(request3, "status", RequestStatus.SYNCRSLT);
        setField(request3, "createdBy", 200L);
        setField(request3, "updatedBy", 200L);
        requestRepository.save(request3);
        log.info("✓ 요청 생성: 창고 ID={}, 상태={}", 2L, RequestStatus.SYNCRSLT);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
