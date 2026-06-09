package com.example.debounce.service

import com.example.debounce.job.DebounceJobRequest
import org.jobrunr.scheduling.JobRequestScheduler
import org.jobrunr.storage.StorageProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class DebounceService(
    private val jobRequestScheduler: JobRequestScheduler,
    private val storageProvider: StorageProvider,
    @Value("\${debounce.delay-seconds:10}") private val delaySeconds: Long
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun scheduleDebounced(key: String): Map<String, Any> {
        val jobId = UUID.nameUUIDFromBytes("upload-settlement".toByteArray())

        try {
            storageProvider.deletePermanently(jobId)
            log.info("[DEBOUNCE] Permanently deleted old job for key=$key, jobId=$jobId")
        } catch (_: Exception) {
            log.info("[DEBOUNCE] No existing job for key=$key")
        }

        val runAt = Instant.now().plusSeconds(delaySeconds)
        jobRequestScheduler.schedule(jobId, runAt, DebounceJobRequest(key))
        log.info("[DEBOUNCE] Scheduled job for key=$key, jobId=$jobId, runAt=$runAt")

        return mapOf(
            "key" to key,
            "jobId" to jobId.toString(),
            "scheduledAt" to Instant.now().toString(),
            "runAt" to runAt.toString(),
            "delaySeconds" to delaySeconds
        )
    }
}
