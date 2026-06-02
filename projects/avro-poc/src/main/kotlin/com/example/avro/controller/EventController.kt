package com.example.avro.controller

import com.example.avro.model.UserEvent
import com.example.avro.producer.UserEventProducer
import mu.KotlinLogging
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/events")
class EventController(
    private val eventProducer: UserEventProducer
) {

    @PostMapping("/produce")
    fun produceEvent(@RequestBody request: ProduceEventRequest): Map<String, Any> {
        logger.info("Received produce request: $request")

        val event = UserEvent(
            id = request.id,
            timestamp = request.timestamp ?: System.currentTimeMillis(),
            eventType = request.eventType,
            userId = request.userId,
            metadata = request.metadata?.let {
                com.example.avro.model.EventMetadata(
                    source = it.source,
                    version = it.version
                )
            }
        )

        eventProducer.send("user-events", request.key ?: event.id, event)

        return mapOf(
            "success" to true,
            "message" to "Event sent successfully",
            "eventId" to event.id,
            "topic" to "user-events"
        )
    }

    @GetMapping("/health")
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "service" to "avro-poc"
        )
    }
}

data class ProduceEventRequest(
    val id: String,
    val key: String? = null,
    val timestamp: Long? = null,
    val eventType: String?,
    val userId: String?,
    val metadata: EventMetadataRequest?
)

data class EventMetadataRequest(
    val source: String?,
    val version: String?
)
