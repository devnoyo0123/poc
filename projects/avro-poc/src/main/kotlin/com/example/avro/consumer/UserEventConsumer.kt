package com.example.avro.consumer

import mu.KotlinLogging
import org.apache.avro.generic.GenericRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class UserEventConsumer {

    @KafkaListener(
        topics = ["\${kafka.topic.user-events:user-events}"],
        groupId = "avro-poc-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consume(
        @Payload message: GenericRecord,
        @Header("kafka_receivedTopic") topic: String,
        @Header("kafka_receivedKey") key: String,
        acknowledgment: Acknowledgment?
    ) {
        logger.info("Received message from topic: $topic, key: $key")
        logger.info("Message: $message")

        // Process the message
        val id = message.get("id").toString()
        val timestamp = message.get("timestamp") as Long
        val eventType = message.get("eventType")?.toString()
        val userId = message.get("userId")?.toString()

        logger.info("Processed event - id: $id, timestamp: $timestamp, eventType: $eventType, userId: $userId")

        // Acknowledge message if manual acknowledgment is enabled
        acknowledgment?.acknowledge()
    }
}
