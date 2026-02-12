package com.example.sse.servlet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example.sse"])
class ServletStackApplication

fun main(args: Array<String>) {
    runApplication<ServletStackApplication>(*args)
}
