package com.example.debounce.service

import com.example.debounce.job.DebounceJobRequest
import org.jobrunr.scheduling.JobRequestScheduler
import org.jobrunr.storage.JobNotFoundException
import org.jobrunr.storage.StorageProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * "디바운스가 deletePermanently 할 때, 이미 SUCCEEDED 된 기존 잡 행을 삭제하는가?" 검증용.
 *
 * 절차:
 *  1) deterministic id로 짧은 지연 schedule → 실제 실행시켜 SUCCEEDED 만듦 (폴링으로 대기)
 *  2) 두 번째 시나리오(디바운스) = deletePermanently(같은 id) → 그 SUCCEEDED 행이 사라지는지 확인
 *  3) 같은 id로 다시 schedule → 새 SCHEDULED 잡 생성 확인
 */
@Service
class SucceededDeletionTestService(
    private val jobRequestScheduler: JobRequestScheduler,
    private val storageProvider: StorageProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun run(key: String): Map<String, Any> {
        val id = UUID.nameUUIDFromBytes("succ-del-$key".toByteArray())
        runCatching { storageProvider.deletePermanently(id) } // 깨끗한 시작

        // --- 1) 실행시켜 SUCCEEDED 만들기 ---
        jobRequestScheduler.schedule(id, Instant.now().plusSeconds(2), DebounceJobRequest(key))
        log.info("[SUCC-DEL] schedule 완료, 실행 대기: {}", id)

        val finalState = waitForState(id, target = "SUCCEEDED", timeoutMs = 25_000)
        log.info("[SUCC-DEL] 1단계 종료 상태: {}", finalState)

        // --- 1.5) PROBE: 삭제 없이 같은 id 재schedule (SUCCEEDED 종료상태가 막는지) ---
        val probeOutcome = runCatching {
            jobRequestScheduler.schedule(id, Instant.now().plusSeconds(600), DebounceJobRequest(key))
        }.fold({ "OK(예외없음)" }, { "EX:${it.javaClass.simpleName}" })
        val stateAfterNoDeleteReschedule = currentState(id)
        log.info("[SUCC-DEL] 삭제없이 재schedule: {} → state={}", probeOutcome, stateAfterNoDeleteReschedule)

        // --- 2) 디바운스: 기존(성공) 행 하드 삭제 ---
        val stateBeforeDelete = currentState(id)
        storageProvider.deletePermanently(id)
        val stateAfterDelete = currentState(id)
        log.info("[SUCC-DEL] 하드 삭제 전: {}, 후: {}", stateBeforeDelete, stateAfterDelete)

        // --- 3) 같은 id로 새 잡 ---
        jobRequestScheduler.schedule(id, Instant.now().plusSeconds(600), DebounceJobRequest(key))
        val stateAfterReschedule = currentState(id)

        runCatching { storageProvider.deletePermanently(id) } // 정리

        val succeededRowDeleted = stateBeforeDelete == "SUCCEEDED" && stateAfterDelete == "NOT_FOUND"
        return mapOf(
            "id" to id.toString(),
            "1_실행후_상태" to finalState,
            "1.5_삭제없이_재schedule_결과" to "$probeOutcome → state=$stateAfterNoDeleteReschedule",
            "2_삭제전_상태" to stateBeforeDelete,
            "3_하드삭제후_상태" to stateAfterDelete,
            "4_재schedule후_상태" to stateAfterReschedule,
            "결론" to "SUCCEEDED 종료상태는 삭제 없이 재schedule하면 $stateAfterNoDeleteReschedule 로 막힘. " +
                "deletePermanently로 행 제거해야 새 잡(SCHEDULED) 생성됨. 이게 production이 하드 삭제 쓰는 이유."
        )
    }

    /** 잡이 target 상태가 될 때까지 폴링. 못 되면 마지막 상태 반환. */
    private fun waitForState(id: UUID, target: String, timeoutMs: Long): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last = "UNKNOWN"
        while (System.currentTimeMillis() < deadline) {
            last = currentState(id)
            if (last == target) return last
            Thread.sleep(500)
        }
        return "$last (timeout, target=$target 도달 못함)"
    }

    /** 현재 잡 상태. 행 없으면 NOT_FOUND. */
    private fun currentState(id: UUID): String = try {
        storageProvider.getJobById(id).state.name
    } catch (e: JobNotFoundException) {
        "NOT_FOUND"
    }
}
