package com.example.queue.service

import com.example.queue.config.QueueProperties
import com.example.queue.domain.QueueHistory
import com.example.queue.domain.QueueItem
import com.example.queue.domain.QueueStatus
import com.example.queue.dto.JoinQueueRequest
import com.example.queue.dto.JoinQueueResponse
import com.example.queue.dto.PositionResponse
import com.example.queue.dto.ProcessResponse
import com.example.queue.dto.QueueStatusResponse
import com.example.queue.exception.AlreadyInQueueException
import com.example.queue.exception.QueueFullException
import com.example.queue.exception.QueueNotFoundException
import com.example.queue.queue.QueueBackend
import com.example.queue.repository.QueueHistoryRepository
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class QueueService(
    private val queueBackend: QueueBackend,
    private val queueProperties: QueueProperties,
    private val queueHistoryRepository: QueueHistoryRepository,
    private val objectMapper: ObjectMapper
) {

    /**
     * Join queue
     */
    fun joinQueue(request: JoinQueueRequest): Mono<JoinQueueResponse> {
        val timestamp = System.currentTimeMillis()
        val queueItem = QueueItem(
            userId = request.userId,
            priority = request.priority,
            metadata = request.metadata
        )

        return queueBackend.getQueueSize()
            .flatMap { size ->
                if (size >= queueProperties.maxSize) {
                    Mono.error(QueueFullException())
                } else {
                    queueBackend.addToQueue(request.userId, timestamp)
                }
            }
            .flatMap { added ->
                if (!added) {
                    Mono.error(AlreadyInQueueException(request.userId))
                } else {
                    queueBackend.getPosition(request.userId)
                }
            }
            .flatMap { position ->
                val pos = (position ?: 0) + 1  // Convert to 1-based position
                val estimatedWaitTime = calculateEstimatedWaitTime(pos)

                logger.info { "User ${request.userId} joined queue at position $pos" }

                // Save to database
                val history = QueueHistory(
                    queueId = queueItem.queueId.toString(),
                    userId = request.userId,
                    status = QueueStatus.WAITING.name,
                    priority = request.priority.name,
                    joinedAt = queueItem.joinedAt,
                    metadata = objectMapper.writeValueAsString(request.metadata)
                )

                queueHistoryRepository.save(history)
                    .then(
                        Mono.just(
                            JoinQueueResponse(
                                queueId = queueItem.queueId,
                                position = pos,
                                estimatedWaitTime = estimatedWaitTime,
                                joinedAt = queueItem.joinedAt
                            )
                        )
                    )
            }
    }

    /**
     * Get position in queue
     */
    fun getPosition(userId: String): Mono<PositionResponse> {
        return queueBackend.getActiveToken(userId)
            .flatMap { token ->
                if (token != null) {
                    // User is already active
                    Mono.just(
                        PositionResponse(
                            queueId = UUID.randomUUID(),
                            currentPosition = 0,
                            estimatedWaitTime = Duration.ZERO,
                            status = QueueStatus.PROCESSING
                        )
                    )
                } else {
                    // Check queue position
                    queueBackend.getPosition(userId)
                        .flatMap { position ->
                            if (position == null) {
                                Mono.error(QueueNotFoundException(userId))
                            } else {
                                val pos = position + 1  // Convert to 1-based
                                val estimatedWaitTime = calculateEstimatedWaitTime(pos)

                                Mono.just(
                                    PositionResponse(
                                        queueId = UUID.randomUUID(),
                                        currentPosition = pos,
                                        estimatedWaitTime = estimatedWaitTime,
                                        status = QueueStatus.WAITING
                                    )
                                )
                            }
                        }
                }
            }
    }

    /**
     * Leave queue
     */
    fun leaveQueue(userId: String): Mono<Void> {
        return queueBackend.removeFromQueue(userId)
            .flatMap { removed ->
                if (!removed) {
                    Mono.error(QueueNotFoundException(userId))
                } else {
                    logger.info { "User $userId left queue" }
                    Mono.empty()
                }
            }
    }

    /**
     * Process next batch from queue
     */
    fun processQueue(): Mono<List<ProcessResponse>> {
        val batchSize = queueProperties.scheduler.batchSize.toLong()
        val processedAt = Instant.now()

        return queueBackend.popNext(batchSize)
            .flatMapMany { userIds ->
                Flux.fromIterable(userIds)
            }
            .flatMap { userId ->
                val token = UUID.randomUUID().toString()
                queueBackend.setActiveToken(userId, token, queueProperties.activeTtl)
                    .then(updateHistoryAsProcessed(userId, processedAt))
                    .map { userId }
            }
            .map { userId ->
                logger.info { "Processed user $userId from queue" }
                ProcessResponse(
                    queueId = UUID.randomUUID(),
                    userId = userId,
                    waitTime = Duration.ZERO
                )
            }
            .collectList()
    }

    private fun updateHistoryAsProcessed(userId: String, processedAt: Instant): Mono<Void> {
        return queueHistoryRepository.findByUserId(userId)
            .filter { it.status == QueueStatus.WAITING.name }
            .next()
            .flatMap { history ->
                val waitTimeSeconds = Duration.between(history.joinedAt, processedAt).seconds.toInt()
                val updated = history.copy(
                    status = QueueStatus.COMPLETED.name,
                    processedAt = processedAt,
                    waitTimeSeconds = waitTimeSeconds
                )
                queueHistoryRepository.save(updated)
            }
            .then()
    }

    /**
     * Get queue status
     */
    fun getQueueStatus(): Mono<QueueStatusResponse> {
        return queueBackend.getQueueSize()
            .map { size ->
                val processingRate = queueProperties.processingRate.toDouble()
                val averageWaitTime = if (size > 0) {
                    Duration.ofSeconds((size / processingRate * 60).toLong())
                } else {
                    Duration.ZERO
                }

                QueueStatusResponse(
                    totalInQueue = size,
                    processingRate = processingRate,
                    averageWaitTime = averageWaitTime,
                    queueStatus = if (size < queueProperties.maxSize) "ACTIVE" else "FULL"
                )
            }
    }

    /**
     * Calculate estimated wait time based on position
     */
    private fun calculateEstimatedWaitTime(position: Long): Duration {
        val processingRate = queueProperties.processingRate.toDouble()  // items per minute
        val waitMinutes = position / processingRate
        return Duration.ofSeconds((waitMinutes * 60).toLong())
    }
}
