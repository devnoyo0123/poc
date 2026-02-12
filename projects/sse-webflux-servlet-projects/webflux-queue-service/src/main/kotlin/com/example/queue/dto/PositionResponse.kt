package com.example.queue.dto

import com.example.queue.domain.QueueStatus
import java.time.Duration
import java.util.UUID

data class PositionResponse(
    val queueId: UUID,
    val currentPosition: Long,
    val estimatedWaitTime: Duration,
    val status: QueueStatus
)
