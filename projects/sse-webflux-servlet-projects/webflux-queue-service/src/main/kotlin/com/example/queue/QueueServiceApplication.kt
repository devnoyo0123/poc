package com.example.queue

import com.example.queue.config.QueueProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(QueueProperties::class)
class QueueServiceApplication

fun main(args: Array<String>) {
    runApplication<QueueServiceApplication>(*args)
}
