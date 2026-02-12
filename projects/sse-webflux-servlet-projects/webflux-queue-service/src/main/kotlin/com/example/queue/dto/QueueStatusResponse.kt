package com.example.queue.dto

import java.time.Duration

data class QueueStatusResponse(
    val totalInQueue: Long,
    val processingRate: Double,
    val averageWaitTime: Duration,
    val queueStatus: String
)
