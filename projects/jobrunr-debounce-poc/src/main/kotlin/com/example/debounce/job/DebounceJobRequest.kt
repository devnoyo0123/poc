package com.example.debounce.job

import org.jobrunr.jobs.lambdas.JobRequest
import org.jobrunr.jobs.lambdas.JobRequestHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

data class DebounceJobRequest(val key: String = "") : JobRequest {
    override fun getJobRequestHandler(): Class<out JobRequestHandler<*>> = DebounceJobHandler::class.java
}

@Component
class DebounceJobHandler : JobRequestHandler<DebounceJobRequest> {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(request: DebounceJobRequest) {
        log.info("==================================================")
        log.info("[JOB EXECUTED] key=${request.key}, executedAt=${Instant.now()}")
        log.info("==================================================")
    }
}