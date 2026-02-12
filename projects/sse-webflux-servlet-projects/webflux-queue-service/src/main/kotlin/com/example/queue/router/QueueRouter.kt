package com.example.queue.router

import com.example.queue.handler.QueueHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class QueueRouter {

    @Bean
    fun queueRoutes(handler: QueueHandler): RouterFunction<ServerResponse> = router {
        "/api/v1/queue".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                POST("/join", handler::joinQueue)
                GET("/position/{userId}", handler::getPosition)
                DELETE("/{userId}", handler::leaveQueue)
                GET("/status", handler::getStatus)
                POST("/process", handler::processQueue)
            }
        }
    }
}
