package com.example.redis.wsgateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.example.redis"])
class WsGatewayApplication

fun main(args: Array<String>) {
    runApplication<WsGatewayApplication>(*args)
}
