package com.example.queue.service

import com.example.queue.config.QueueProperties
import com.example.queue.config.SchedulerProperties
import com.example.queue.domain.Priority
import com.example.queue.dto.JoinQueueRequest
import com.example.queue.exception.AlreadyInQueueException
import com.example.queue.exception.QueueFullException
import com.example.queue.queue.QueueBackend
import com.example.queue.repository.QueueHistoryRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class QueueServiceTest {

    private lateinit var queueBackend: QueueBackend
    private lateinit var queueProperties: QueueProperties
    private lateinit var queueHistoryRepository: QueueHistoryRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var queueService: QueueService

    @BeforeEach
    fun setup() {
        queueBackend = mockk()
        queueHistoryRepository = mockk(relaxed = true)
        objectMapper = ObjectMapper()
        queueProperties = QueueProperties(
            maxSize = 100,
            processingRate = 10,
            expirationTime = 1800,
            activeTtl = 300,
            scheduler = SchedulerProperties(
                processInterval = 5000,
                batchSize = 10
            )
        )
        queueService = QueueService(queueBackend, queueProperties, queueHistoryRepository, objectMapper)
    }

    @Test
    fun `joinQueue should successfully add user to queue`() {
        // Given
        val request = JoinQueueRequest(userId = "user123", priority = Priority.NORMAL)
        every { queueBackend.getQueueSize() } returns Mono.just(50L)
        every { queueBackend.addToQueue(any(), any()) } returns Mono.just(true)
        every { queueBackend.getPosition("user123") } returns Mono.just(49L)
        every { queueHistoryRepository.save(any()) } returns Mono.empty()

        // When & Then
        StepVerifier.create(queueService.joinQueue(request))
            .expectNextMatches { response ->
                response.position == 50L &&
                response.queueId != null
            }
            .verifyComplete()

        verify(exactly = 1) { queueBackend.addToQueue("user123", any()) }
    }

    @Test
    fun `joinQueue should fail when queue is full`() {
        // Given
        val request = JoinQueueRequest(userId = "user123", priority = Priority.NORMAL)
        every { queueBackend.getQueueSize() } returns Mono.just(100L)

        // When & Then
        StepVerifier.create(queueService.joinQueue(request))
            .expectError(QueueFullException::class.java)
            .verify()
    }

    @Test
    fun `joinQueue should fail when user already in queue`() {
        // Given
        val request = JoinQueueRequest(userId = "user123", priority = Priority.NORMAL)
        every { queueBackend.getQueueSize() } returns Mono.just(50L)
        every { queueBackend.addToQueue(any(), any()) } returns Mono.just(false)

        // When & Then
        StepVerifier.create(queueService.joinQueue(request))
            .expectError(AlreadyInQueueException::class.java)
            .verify()
    }

    @Test
    fun `getQueueStatus should return current queue metrics`() {
        // Given
        every { queueBackend.getQueueSize() } returns Mono.just(25L)

        // When & Then
        StepVerifier.create(queueService.getQueueStatus())
            .expectNextMatches { response ->
                response.totalInQueue == 25L &&
                response.queueStatus == "ACTIVE" &&
                response.processingRate == 10.0
            }
            .verifyComplete()
    }

    @Test
    fun `leaveQueue should remove user from queue`() {
        // Given
        every { queueBackend.removeFromQueue("user123") } returns Mono.just(true)

        // When & Then
        StepVerifier.create(queueService.leaveQueue("user123"))
            .verifyComplete()

        verify(exactly = 1) { queueBackend.removeFromQueue("user123") }
    }
}
