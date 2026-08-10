package com.example.redis.apiserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example.redis"])
class ApiServerApplication

fun main(args: Array<String>) {
    runApplication<ApiServerApplication>(*args)
}
