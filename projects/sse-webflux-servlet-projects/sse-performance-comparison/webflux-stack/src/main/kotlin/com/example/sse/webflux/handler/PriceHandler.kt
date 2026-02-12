package com.example.sse.webflux.handler

import com.example.sse.domain.StockPrice
import com.example.sse.service.PriceGenerator
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicLong

@Component
class PriceHandler(
    private val priceGenerator: PriceGenerator,
    private val meterRegistry: MeterRegistry
) {

    private val activeConnections = AtomicLong(0)
    private val totalConnections: Counter
    private val connectionGauge: Gauge

    init {
        totalConnections = Counter.builder("sse.webflux.connections.total")
            .description("Total number of SSE connections established")
            .register(meterRegistry)

        connectionGauge = Gauge.builder("sse.webflux.connections.active", activeConnections, { it.get().toDouble() })
            .description("Current number of active SSE connections")
            .register(meterRegistry)
    }

    fun streamPrices(request: ServerRequest): Mono<ServerResponse> {
        totalConnections.increment()
        activeConnections.incrementAndGet()

        val priceFlux = priceGenerator.subscribeToAll()
            .map { price ->
                ServerSentEvent.builder(price)
                    .id("${price.symbol}-${price.timestamp.toEpochMilli()}")
                    .event("price-update")
                    .build()
            }
            .doOnCancel { activeConnections.decrementAndGet() }
            .doOnComplete { activeConnections.decrementAndGet() }
            .doOnError { activeConnections.decrementAndGet() }

        return ServerResponse.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(priceFlux, ServerSentEvent::class.java)
    }

    fun streamSymbol(request: ServerRequest): Mono<ServerResponse> {
        val symbol = request.pathVariable("symbol")
        totalConnections.increment()
        activeConnections.incrementAndGet()

        val priceFlux = priceGenerator.subscribeToSymbol(symbol.uppercase())
            .map { price ->
                ServerSentEvent.builder(price)
                    .id("${price.symbol}-${price.timestamp.toEpochMilli()}")
                    .event("price-update")
                    .build()
            }
            .doOnCancel { activeConnections.decrementAndGet() }
            .doOnComplete { activeConnections.decrementAndGet() }
            .doOnError { activeConnections.decrementAndGet() }

        return ServerResponse.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(priceFlux, ServerSentEvent::class.java)
    }

    fun getStats(request: ServerRequest): Mono<ServerResponse> {
        val stats = mapOf(
            "activeConnections" to activeConnections.get(),
            "totalConnections" to totalConnections.count(),
            "stack" to "webflux"
        )
        return ServerResponse.ok()
            .bodyValue(stats)
    }
}
