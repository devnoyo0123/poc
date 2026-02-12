package com.example.queue.repository

import com.example.queue.domain.QueueHistory
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface QueueHistoryRepository : R2dbcRepository<QueueHistory, UUID> {

    fun findByUserId(userId: String): Flux<QueueHistory>

    fun findByQueueId(queueId: String): Mono<QueueHistory>

    @Query("""
        SELECT * FROM queue_history
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    fun findRecentHistory(limit: Int): Flux<QueueHistory>

    @Query("""
        SELECT AVG(wait_time_seconds) as avg_wait_time
        FROM queue_history
        WHERE status = 'COMPLETED'
        AND processed_at IS NOT NULL
    """)
    fun getAverageWaitTimeSeconds(): Mono<Double>
}
