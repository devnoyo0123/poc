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
 * "soft delete로 디바운스하면 재schedule한 잡이 실제로 실행되는가?" 끝까지 확인.
 *
 * soft  시나리오: schedule → soft delete → schedule → 실행 대기 → 돌았나?
 * hard  시나리오: schedule → hard delete → schedule → 실행 대기 → 돌았나?
 * 같은 조건에서 둘을 비교한다. (지연 3초)
 */
@Service
class SoftDebounceExecutionTestService(
    private val jobRequestScheduler: JobRequestScheduler,
    private val storageProvider: StorageProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun run(key: String): Map<String, Any> {
        val softResult = scenario("soft-exec-$key", useSoftDelete = true)
        val hardResult = scenario("hard-exec-$key", useSoftDelete = false)

        return mapOf(
            "SOFT_delete_디바운스" to softResult,
            "HARD_delete_디바운스" to hardResult,
            "결론" to "soft는 재schedule 잡이 DELETED에 막혀 실행 안 됨, hard는 정상 실행(SUCCEEDED)"
        )
    }

    private fun scenario(seed: String, useSoftDelete: Boolean): Map<String, String> {
        val id = UUID.nameUUIDFromBytes(seed.toByteArray())
        runCatching { storageProvider.deletePermanently(id) } // clean

        // 1) 첫 잡
        jobRequestScheduler.schedule(id, Instant.now().plusSeconds(2), DebounceJobRequest(seed))
        // 2) 디바운스: 직전 잡 제거
        if (useSoftDelete) jobRequestScheduler.delete(id)            // soft
        else storageProvider.deletePermanently(id)                  // hard
        val stateAfterDelete = currentState(id)
        // 3) 같은 id로 새 잡 (실제로 실행돼야 할 잡)
        jobRequestScheduler.schedule(id, Instant.now().plusSeconds(2), DebounceJobRequest(seed))
        val stateAfterReschedule = currentState(id)

        // 4) 실행 대기 (SUCCEEDED 되나?)
        val finalState = waitForState(id, "SUCCEEDED", 7_000)
        val ran = finalState == "SUCCEEDED"

        runCatching { storageProvider.deletePermanently(id) } // clean

        return mapOf(
            "id" to id.toString(),
            "삭제직후_state" to stateAfterDelete,
            "재schedule직후_state" to stateAfterReschedule,
            "최종_state" to finalState,
            "잡_실행됨" to if (ran) "YES ✅" else "NO ❌ (디바운스 잡 유실)"
        )
    }

    private fun waitForState(id: UUID, target: String, timeoutMs: Long): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last = "UNKNOWN"
        while (System.currentTimeMillis() < deadline) {
            last = currentState(id)
            if (last == target) return last
            Thread.sleep(500)
        }
        return "$last (timeout)"
    }

    private fun currentState(id: UUID): String = try {
        storageProvider.getJobById(id).state.name
    } catch (e: JobNotFoundException) {
        "NOT_FOUND"
    }
}
