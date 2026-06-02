package com.example.avro.config

import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaConfig(
    @Value("\${app.env}") private val env: String,
    @Value("\${kafka.bootstrap-servers}") private val bootstrapServers: String
) {

    @Bean
    fun producerProps(): MutableMap<String, Any> {
        return mutableMapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to "org.apache.kafka.common.serialization.StringSerializer",
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to "io.confluent.kafka.serializers.KafkaAvroSerializer",
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            "schema.registry.url" to "http://localhost:8081"
        )
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, GenericRecord> {
        return DefaultKafkaProducerFactory(producerProps())
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, GenericRecord> {
        return KafkaTemplate(producerFactory())
    }
}
