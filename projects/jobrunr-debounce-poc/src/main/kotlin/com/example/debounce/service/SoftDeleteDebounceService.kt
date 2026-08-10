package com.example.debounce.service

import com.example.debounce.job.DebounceJobRequest
import org.jobrunr.scheduling.JobRequestScheduler
import org.jobrunr.storage.StorageProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Deterministic id 기반 디바운스 (멀티 인스턴스 안전).
 *
 * 변경 전(랜덤 UUID + 소프트 삭제 + @Volatile in-memory 추적)의 문제:
 *  - 직전 jobId를 JVM 메모리 변수로 기억 → 인스턴스마다 따로 → 멀티 인스턴스에서 디바운스 깨짐.
 *
 * 변경 후:
 *  - key에서 항상 같은 UUID 계산 → "기억" 불필요, 모든 인스턴스가 같은 id 도출 (DB가 진실).
 *  - 고정 id는 jobrunr_jobs PK로 재사용됨 → soft delete(행 잔존) 시 동일 PK 재insert가 제약 위반.
 *    따라서 deletePermanently(하드 삭제)로 행을 비운 뒤 다시 schedule 해야 함.
 */
@Service
class SoftDeleteDebounceService(
    private val jobRequestScheduler: JobRequestScheduler,
    private val storageProvider: StorageProvider,
    @Value("\${debounce.delay-seconds:10}") private val delaySeconds: Long
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun scheduleDebounced(key: String): Map<String, Any> {
        // 1. key로 결정론적 id 계산 (DB 조회/메모리 추적 없음)
        val jobId = UUID.nameUUIDFromBytes("debounce-$key".toByteArray())

        // 2. 기존 잡 하드 삭제 (PK 비우기). 없으면 무시.
        try {
            storageProvider.deletePermanently(jobId)
            log.info("[DEBOUNCE] 기존 잡 하드 삭제: id={}, key={}", jobId, key)
        } catch (e: Exception) {
            log.debug("[DEBOUNCE] 기존 잡 없음 (무시): {}", e.message)
        }

        // 3. 같은 id로 새 schedule (이제 PK 충돌 없음)
        val runAt = Instant.now().plusSeconds(delaySeconds)
        jobRequestScheduler.schedule(jobId, runAt, DebounceJobRequest(key))
        log.info("[DEBOUNCE] 새 잡 schedule: id={}, key={}, runAt={}", jobId, key, runAt)

        return mapOf(
            "key" to key,
            "jobId" to jobId.toString(),
            "scheduledAt" to Instant.now().toString(),
            "runAt" to runAt.toString(),
            "delaySeconds" to delaySeconds
        )
    }
}
