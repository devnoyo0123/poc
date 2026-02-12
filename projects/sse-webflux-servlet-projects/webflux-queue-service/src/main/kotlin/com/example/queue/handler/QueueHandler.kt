package com.example.queue.handler

import com.example.queue.dto.JoinQueueRequest
import com.example.queue.service.QueueService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import reactor.core.publisher.Mono

private val logger = KotlinLogging.logger {}

@Component
class QueueHandler(
    private val queueService: QueueService
) {

    fun joinQueue(request: ServerRequest): Mono<ServerResponse> {
        return request.bodyToMono(JoinQueueRequest::class.java)
            .flatMap { joinRequest ->
                queueService.joinQueue(joinRequest)
            }
            .flatMap { response ->
                ServerResponse.ok().bodyValue(response)
            }
    }

    fun getPosition(request: ServerRequest): Mono<ServerResponse> {
        val userId = request.pathVariable("userId")

        return queueService.getPosition(userId)
            .flatMap { response ->
                ServerResponse.ok().bodyValue(response)
            }
    }

    fun leaveQueue(request: ServerRequest): Mono<ServerResponse> {
        val userId = request.pathVariable("userId")

        return queueService.leaveQueue(userId)
            .then(ServerResponse.noContent().build())
    }

    fun getStatus(request: ServerRequest): Mono<ServerResponse> {
        return queueService.getQueueStatus()
            .flatMap { response ->
                ServerResponse.ok().bodyValue(response)
            }
    }

    fun processQueue(request: ServerRequest): Mono<ServerResponse> {
        return queueService.processQueue()
            .flatMap { response ->
                ServerResponse.ok().bodyValue(response)
            }
    }
}
