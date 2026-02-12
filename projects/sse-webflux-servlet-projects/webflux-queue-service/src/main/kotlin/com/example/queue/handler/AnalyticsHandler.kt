package com.example.queue.handler

import com.example.queue.service.AnalyticsService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Component
class AnalyticsHandler(
    private val analyticsService: AnalyticsService
) {

    fun getAverageWaitTime(request: ServerRequest): Mono<ServerResponse> {
        return analyticsService.getAverageWaitTime()
            .flatMap { response ->
                ServerResponse.ok().bodyValue(response)
            }
    }

    fun getStatistics(request: ServerRequest): Mono<ServerResponse> {
        return analyticsService.getQueueStatistics()
            .flatMap { response ->
                ServerResponse.ok().bodyValue(response)
            }
    }

    fun getUserHistory(request: ServerRequest): Mono<ServerResponse> {
        val userId = request.pathVariable("userId")

        return analyticsService.getUserHistory(userId)
            .collectList()
            .flatMap { history ->
                ServerResponse.ok().bodyValue(history)
            }
    }

    fun getRecentHistory(request: ServerRequest): Mono<ServerResponse> {
        val limit = request.queryParam("limit")
            .map { it.toInt() }
            .orElse(10)

        return analyticsService.getRecentHistory(limit)
            .collectList()
            .flatMap { history ->
                ServerResponse.ok().bodyValue(history)
            }
    }
}
