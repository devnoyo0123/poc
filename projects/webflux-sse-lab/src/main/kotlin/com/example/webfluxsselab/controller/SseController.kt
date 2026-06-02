package com.example.webfluxsselab.controller

import com.example.webfluxsselab.model.Notification
import com.example.webfluxsselab.model.StockPrice
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicLong

@RestController
@RequestMapping("/api/sse")
class SseController {

    private val notificationId = AtomicLong(1)

    /**
     * 실습 1: 기본 SSE - 1초마다 알림 전송
     *
     * 테스트 방법:
     * curl -N http://localhost:8080/api/sse/notifications
     */
    @GetMapping("/notifications", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun notifications(): Flux<ServerSentEvent<String>> {
        return Flux.interval(Duration.ofSeconds(1))
            .map { seq ->
                ServerSentEvent.builder<String>()
                    .id(seq.toString())
                    .event("notification")
                    .data("알림 #${seq} - ${LocalDateTime.now()}")
                    .build()
            }
            .log()
    }

    /**
     * 실습 2: 객체 SSE - Notification 객체 전송
     *
     * 테스트 방법:
     * curl -N http://localhost:8080/api/sse/notifications/object
     */
    @GetMapping("/notifications/object", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun notificationObjects(): Flux<ServerSentEvent<Notification>> {
        return Flux.interval(Duration.ofSeconds(2))
            .map {
                val notification = Notification(
                    id = notificationId.getAndIncrement(),
                    message = "새 알림 #${notificationId.get()}"
                )
                ServerSentEvent.builder<Notification>()
                    .id(notification.id.toString())
                    .event("notification")
                    .data(notification)
                    .build()
            }
    }

    /**
     * 실습 3: 주식 가격 스트리밍 (실시간 업데이트)
     *
     * 테스트 방법:
     * curl -N http://localhost:8080/api/sse/stock/AAPL
     */
    @GetMapping("/stock/{symbol}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stockPrice(@PathVariable symbol: String): Flux<ServerSentEvent<StockPrice>> {
        return Flux.interval(Duration.ofMillis(500))
            .map {
                val basePrice = when (symbol) {
                    "AAPL" -> 150.0
                    "GOOGL" -> 2800.0
                    "TSLA" -> 800.0
                    else -> 100.0
                }
                val change = (Math.random() - 0.5) * 10
                val price = basePrice + change

                StockPrice(
                    symbol = symbol,
                    price = price,
                    change = change
                )
            }
            .map { stockPrice ->
                ServerSentEvent.builder<StockPrice>()
                    .id("${stockPrice.symbol}-${stockPrice.timestamp}")
                    .event("price-update")
                    .data(stockPrice)
                    .build()
            }
    }
}
