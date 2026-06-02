package com.example.webfluxsselab

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WebFluxSseLabApplication

fun main(args: Array<String>) {
    runApplication<WebFluxSseLabApplication>(*args)
}
