package com.example.queue.router

import com.example.queue.handler.AnalyticsHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class AnalyticsRouter {

    @Bean
    fun analyticsRoutes(handler: AnalyticsHandler): RouterFunction<ServerResponse> = router {
        "/api/v1/analytics".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                GET("/wait-time", handler::getAverageWaitTime)
                GET("/statistics", handler::getStatistics)
                GET("/history/{userId}", handler::getUserHistory)
                GET("/recent", handler::getRecentHistory)
            }
        }
    }
}
