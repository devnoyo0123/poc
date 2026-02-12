package com.example.queue.domain

import java.time.Instant
import java.util.UUID

data class QueueItem(
    val queueId: UUID = UUID.randomUUID(),
    val userId: String,
    val priority: Priority = Priority.NORMAL,
    val status: QueueStatus = QueueStatus.WAITING,
    val joinedAt: Instant = Instant.now(),
    val processedAt: Instant? = null,
    val metadata: Map<String, Any> = emptyMap()
)
