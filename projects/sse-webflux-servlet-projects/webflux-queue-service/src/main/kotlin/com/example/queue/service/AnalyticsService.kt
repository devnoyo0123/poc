package com.example.queue.service

import com.example.queue.dto.AverageWaitTimeResponse
import com.example.queue.dto.QueueHistoryResponse
import com.example.queue.dto.QueueStatistics
import com.example.queue.queue.QueueBackend
import com.example.queue.repository.QueueHistoryRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

private val logger = KotlinLogging.logger {}

@Service
class AnalyticsService(
    private val queueHistoryRepository: QueueHistoryRepository,
    private val queueBackend: QueueBackend
) {

    fun getAverageWaitTime(): Mono<AverageWaitTimeResponse> {
        return queueHistoryRepository.getAverageWaitTimeSeconds()
            .flatMap { avgSeconds ->
                queueHistoryRepository.count()
                    .map { count ->
                        AverageWaitTimeResponse(
                            averageWaitTime = Duration.ofSeconds(avgSeconds.toLong()),
                            sampleSize = count
                        )
                    }
            }
            .defaultIfEmpty(
                AverageWaitTimeResponse(
                    averageWaitTime = Duration.ZERO,
                    sampleSize = 0
                )
            )
    }

    fun getQueueStatistics(): Mono<QueueStatistics> {
        return Mono.zip(
            queueHistoryRepository.count(),
            queueHistoryRepository.getAverageWaitTimeSeconds().defaultIfEmpty(0.0),
            queueBackend.getQueueSize()
        ).map { tuple ->
            QueueStatistics(
                totalProcessed = tuple.t1,
                averageWaitTime = Duration.ofSeconds(tuple.t2.toLong()),
                currentQueueSize = tuple.t3
            )
        }
    }

    fun getUserHistory(userId: String): Flux<QueueHistoryResponse> {
        return queueHistoryRepository.findByUserId(userId)
            .map { history ->
                QueueHistoryResponse(
                    queueId = history.queueId,
                    status = history.status,
                    joinedAt = history.joinedAt,
                    processedAt = history.processedAt,
                    waitTime = history.waitTimeSeconds?.let { Duration.ofSeconds(it.toLong()) }
                )
            }
    }

    fun getRecentHistory(limit: Int): Flux<QueueHistoryResponse> {
        return queueHistoryRepository.findRecentHistory(limit)
            .map { history ->
                QueueHistoryResponse(
                    queueId = history.queueId,
                    status = history.status,
                    joinedAt = history.joinedAt,
                    processedAt = history.processedAt,
                    waitTime = history.waitTimeSeconds?.let { Duration.ofSeconds(it.toLong()) }
                )
            }
    }
}
