package com.example.sse.webflux.config

import com.example.sse.webflux.handler.PriceHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router

@Configuration
class RouterConfig(private val priceHandler: PriceHandler) {

    @Bean
    fun priceRouter() = router {
        "/api/prices".nest {
            GET("/stream", priceHandler::streamPrices)
            GET("/stream/{symbol}", priceHandler::streamSymbol)
            GET("/stats", priceHandler::getStats)
        }
    }
}
