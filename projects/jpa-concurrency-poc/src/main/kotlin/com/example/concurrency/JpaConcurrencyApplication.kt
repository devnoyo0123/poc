package com.example.concurrency

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JpaConcurrencyApplication

fun main(args: Array<String>) {
    runApplication<JpaConcurrencyApplication>(*args)
}
