package com.example.queue.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "queue")
data class QueueProperties(
    val maxSize: Int = 1000,
    val processingRate: Int = 10,
    val expirationTime: Long = 1800,
    val activeTtl: Long = 300,
    val scheduler: SchedulerProperties = SchedulerProperties()
)

data class SchedulerProperties(
    val processInterval: Long = 5000,
    val batchSize: Int = 10
)
