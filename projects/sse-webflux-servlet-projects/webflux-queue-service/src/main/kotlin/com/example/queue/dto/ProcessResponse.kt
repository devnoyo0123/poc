package com.example.queue.dto

import java.time.Duration
import java.util.UUID

data class ProcessResponse(
    val queueId: UUID,
    val userId: String,
    val waitTime: Duration
)
