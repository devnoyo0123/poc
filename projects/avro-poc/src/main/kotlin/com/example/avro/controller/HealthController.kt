package com.example.avro.controller

import mu.KotlinLogging
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class HealthController {

    @GetMapping("/health")
    fun health(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "service" to "avro-poc",
            "timestamp" to System.currentTimeMillis()
        )
    }

    @GetMapping("/info")
    fun info(): Map<String, String> {
        return mapOf(
            "name" to "avro-poc",
            "description" to "Kafka Schema Registry POC",
            "version" to "0.0.1-SNAPSHOT",
            "kafkaUI" to "http://localhost:8080",
            "schemaRegistry" to "http://localhost:8081"
        )
    }
}
