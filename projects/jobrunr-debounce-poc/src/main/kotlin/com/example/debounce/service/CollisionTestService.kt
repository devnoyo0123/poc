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
 * JobRunr가 같은 id 재schedule / soft delete를 실제로 어떻게 처리하는지 단계별 정밀 진단.
 * 각 단계: 액션 실행(성공/예외) + 직후 DB 상태(state) 기록.
 */
@Service
class CollisionTestService(
    private val jobRequestScheduler: JobRequestScheduler,
    private val storageProvider: StorageProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun run(key: String): Map<String, Any> {
        val id = UUID.nameUUIDFromBytes("diag-$key".toByteArray())
        runCatching { storageProvider.deletePermanently(id) } // clean

        val steps = LinkedHashMap<String, String>()

        rec(steps, id, "A_최초_schedule") {
            jobRequestScheduler.schedule(id, Instant.now().plusSeconds(600), DebounceJobRequest(key))
        }
        rec(steps, id, "B_delete없이_같은id_재schedule") {
            jobRequestScheduler.schedule(id, Instant.now().plusSeconds(600), DebounceJobRequest(key))
        }
        rec(steps, id, "C_soft_delete") {
            jobRequestScheduler.delete(id)
        }
        rec(steps, id, "D_soft_delete후_같은id_재schedule") {
            jobRequestScheduler.schedule(id, Instant.now().plusSeconds(600), DebounceJobRequest(key))
        }

        runCatching { storageProvider.deletePermanently(id) } // clean

        return mapOf("id" to id.toString(), "steps" to steps)
    }

    /** 액션 실행 결과 + 직후 state 를 한 줄로 기록. */
    private fun rec(into: MutableMap<String, String>, id: UUID, label: String, action: () -> Unit) {
        val outcome = runCatching { action() }
            .fold({ "OK" }, { "EX:${it.javaClass.simpleName}:${it.message?.take(120)}" })
        into[label] = "$outcome | state=${currentState(id)}"
        log.info("[DIAG] {} -> {}", label, into[label])
    }

    private fun currentState(id: UUID): String = try {
        storageProvider.getJobById(id).state.name
    } catch (e: JobNotFoundException) {
        "NOT_FOUND"
    }
}
