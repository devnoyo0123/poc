package com.example.queue.dto

import java.time.Duration
import java.time.Instant
import java.util.UUID

data class JoinQueueResponse(
    val queueId: UUID,
    val position: Long,
    val estimatedWaitTime: Duration,
    val joinedAt: Instant
)
