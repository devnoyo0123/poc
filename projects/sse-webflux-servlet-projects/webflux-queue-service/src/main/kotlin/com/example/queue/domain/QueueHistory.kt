package com.example.queue.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("queue_history")
data class QueueHistory(
    @Id
    val id: UUID? = null,

    @Column("queue_id")
    val queueId: String,

    @Column("user_id")
    val userId: String,

    @Column("status")
    val status: String,

    @Column("priority")
    val priority: String,

    @Column("joined_at")
    val joinedAt: Instant,

    @Column("processed_at")
    val processedAt: Instant? = null,

    @Column("wait_time_seconds")
    val waitTimeSeconds: Int? = null,

    @Column("metadata")
    val metadata: String? = null,  // JSON string

    @Column("created_at")
    val createdAt: Instant = Instant.now()
)
