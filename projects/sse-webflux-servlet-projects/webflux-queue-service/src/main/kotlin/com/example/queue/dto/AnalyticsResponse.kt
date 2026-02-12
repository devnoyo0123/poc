package com.example.queue.dto

import java.time.Duration
import java.time.Instant

data class AverageWaitTimeResponse(
    val averageWaitTime: Duration,
    val sampleSize: Long,
    val period: String = "P1D"  // ISO-8601 duration
)

data class QueueHistoryResponse(
    val queueId: String,
    val status: String,
    val joinedAt: Instant,
    val processedAt: Instant?,
    val waitTime: Duration?
)

data class QueueStatistics(
    val totalProcessed: Long,
    val averageWaitTime: Duration,
    val currentQueueSize: Long
)
