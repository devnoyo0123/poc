package com.example.kafkareactor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KafkaReactorApplication

fun main(args: Array<String>) {
    runApplication<KafkaReactorApplication>(*args)
}
