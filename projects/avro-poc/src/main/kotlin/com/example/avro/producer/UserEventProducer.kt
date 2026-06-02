package com.example.avro.producer

import com.example.avro.model.UserEvent
import com.example.avro.model.toGenericRecord
import mu.KotlinLogging
import org.apache.avro.generic.GenericRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class UserEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, GenericRecord>
) {

    fun send(topic: String, key: String, event: UserEvent) {
        logger.info("Sending event to topic: $topic, key: $key, event: $event")

        val record = kafkaTemplate.send(topic, key, event.toGenericRecord())

        record.whenComplete { _, ex ->
            if (ex == null) {
                logger.info("Message sent successfully to topic: $topic")
            } else {
                logger.error("Failed to send message to topic: $topic", ex)
            }
        }
    }

    fun sendGenericRecord(topic: String, key: String, record: GenericRecord) {
        logger.info("Sending generic record to topic: $topic, key: $key")

        val future = kafkaTemplate.send(topic, key, record)

        future.whenComplete { _, ex ->
            if (ex == null) {
                logger.info("Message sent successfully to topic: $topic")
            } else {
                logger.error("Failed to send message to topic: $topic", ex)
            }
        }
    }
}
