package com.example.s3sqs.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class SqsEventListener(
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @SqsListener("\${app.aws.sqs.queue-name}")
    fun handleMessage(@Payload message: String) {
        log.info("=== SQS Message Received ===")
        log.info("Raw message: {}", message)

        try {
            val jsonNode = objectMapper.readTree(message)

            // S3 이벤트 알림 메시지 파싱
            val records = jsonNode.path("Records")
            if (records.isArray && records.size() > 0) {
                val record = records[0]
                val eventName = record.path("eventName").asText()
                val bucket = record.path("s3").path("bucket").path("name").asText()
                val key = record.path("s3").path("object").path("key").asText()
                val size = record.path("s3").path("object").path("size").asLong()

                log.info("S3 Event: {}", eventName)
                log.info("Bucket: {}, Key: {}, Size: {} bytes", bucket, key, size)
            }
        } catch (e: Exception) {
            log.error("Failed to parse SQS message", e)
        }

        log.info("=== Message Processing Complete ===")
    }
}
