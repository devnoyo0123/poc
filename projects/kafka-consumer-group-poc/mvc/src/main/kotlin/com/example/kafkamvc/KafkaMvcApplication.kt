package com.example.kafkamvc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KafkaMvcApplication

fun main(args: Array<String>) {
    runApplication<KafkaMvcApplication>(*args)
}
