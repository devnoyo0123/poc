package com.example.sse.webflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example.sse"])
class WebFluxStackApplication

fun main(args: Array<String>) {
    runApplication<WebFluxStackApplication>(*args)
}
