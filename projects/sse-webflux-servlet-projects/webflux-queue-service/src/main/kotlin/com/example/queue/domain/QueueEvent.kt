package com.example.queue.domain

import java.time.Instant
import java.util.UUID

sealed class QueueEvent(
    open val queueId: UUID,
    open val userId: String,
    open val timestamp: Instant = Instant.now()
) {
    data class Joined(
        override val queueId: UUID,
        override val userId: String,
        val position: Long,
        override val timestamp: Instant = Instant.now()
    ) : QueueEvent(queueId, userId, timestamp)

    data class PositionUpdated(
        override val queueId: UUID,
        override val userId: String,
        val newPosition: Long,
        override val timestamp: Instant = Instant.now()
    ) : QueueEvent(queueId, userId, timestamp)

    data class Processed(
        override val queueId: UUID,
        override val userId: String,
        val waitTime: Long,
        override val timestamp: Instant = Instant.now()
    ) : QueueEvent(queueId, userId, timestamp)
}
