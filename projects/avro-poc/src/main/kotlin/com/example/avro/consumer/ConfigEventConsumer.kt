package com.example.avro.consumer

import com.example.avro.config.AppConfig
import com.example.avro.model.ConfigChangeEvent
import mu.KotlinLogging
import org.apache.avro.generic.GenericRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.refresh.ContextRefresher
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class ConfigEventConsumer(
    private val appConfig: AppConfig,
    private val contextRefresher: ContextRefresher,
    private val environment: ConfigurableEnvironment,
    @Value("\${server.port}") private val serverPort: String
) {

    @KafkaListener(
        topics = ["\${kafka.topic.config-events:config-events}"],
        groupId = "config-consumer-group-\${server.port}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeConfigEvent(
        @Payload message: GenericRecord,
        @Header("kafka_receivedTopic") topic: String,
        @Header("kafka_receivedMessageKey") key: String,
        acknowledgment: Acknowledgment
    ) {
        logger.info("🔧 Received config event from topic: $topic, key: $key")
        logger.info("📋 Message: $message")

        try {
            // ConfigChangeEvent로 변환
            val event = toConfigChangeEvent(message)

            // Environment에 Property 직접 수정
            updateEnvironmentProperty(event)

            // Context refresh (Spring Cloud Bus의 핵심!)
            logger.info("🔄 Refreshing Spring context...")
            val refreshedKeys = contextRefresher.refresh()
            logger.info("✅ Context refreshed! Updated keys: $refreshedKeys")

            // 수동 커밋
            acknowledgment.acknowledge()
            logger.info("✅ Message acknowledged")
        } catch (e: Exception) {
            logger.error("❌ Failed to process config event", e)
        }
    }

    private fun updateEnvironmentProperty(event: ConfigChangeEvent) {
        val propertyName = when (event.configType) {
            "featureFlag" -> "app.feature-flags.${event.key}"
            "timeout" -> "app.timeouts.${event.key}"
            "limit" -> "app.limits.${event.key}"
            else -> return
        }

        val propertyValue = event.value.toString()

        logger.info("📝 Updating property: $propertyName = $propertyValue")

        // Environment의 propertySources 맨 앞에 새로운 PropertySource 추가
        val mutableProps = mutableMapOf<String, Any>(propertyName to propertyValue)
        val propertySource = MapPropertySource("kafka-config-events", mutableProps)

        environment.propertySources.addFirst(propertySource)

        logger.info("✅ Property added to Environment")
    }

    private fun toConfigChangeEvent(record: GenericRecord): ConfigChangeEvent {
        return ConfigChangeEvent(
            configType = record.get("configType").toString(),
            key = record.get("key").toString(),
            value = record.get("value"),
            timestamp = record.get("timestamp") as Long,
            source = record.get("source")?.toString()
        )
    }
}
