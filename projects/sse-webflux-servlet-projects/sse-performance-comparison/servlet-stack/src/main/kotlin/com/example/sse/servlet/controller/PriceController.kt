package com.example.sse.servlet.controller

import com.example.sse.domain.StockPrice
import com.example.sse.service.PriceGenerator
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

@RestController
@RequestMapping("/api/prices")
class PriceController(
    private val priceGenerator: PriceGenerator,
    private val meterRegistry: MeterRegistry
) {

    private val activeConnections = AtomicLong(0)
    private val totalConnections: Counter
    private val connectionGauge: Gauge

    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "sse-servlet-${System.currentTimeMillis()}")
    }

    init {
        totalConnections = Counter.builder("sse.servlet.connections.total")
            .description("Total number of SSE connections established")
            .register(meterRegistry)

        connectionGauge = Gauge.builder("sse.servlet.connections.active", activeConnections, { it.get().toDouble() })
            .description("Current number of active SSE connections")
            .register(meterRegistry)
    }

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamPrices(): SseEmitter {
        totalConnections.increment()
        activeConnections.incrementAndGet()

        val emitter = SseEmitter(Long.MAX_VALUE)

        val subscription = priceGenerator.subscribeToAll()
            .doOnNext { price ->
                try {
                    emitter.send(
                        SseEmitter.event()
                            .data(price)
                            .id("${price.symbol}-${price.timestamp.toEpochMilli()}")
                            .comment("Stock price update")
                    )
                } catch (e: Exception) {
                    emitter.completeWithError(e)
                }
            }
            .doOnCancel {
                cleanup(emitter)
            }
            .doOnComplete {
                cleanup(emitter)
            }
            .doOnError {
                cleanup(emitter)
            }
            .subscribe()

        emitter.onCompletion {
            subscription.dispose()
            cleanup(null)
        }

        emitter.onTimeout {
            subscription.dispose()
            cleanup(null)
        }

        emitter.onError { ex ->
            subscription.dispose()
            cleanup(null)
        }

        return emitter
    }

    @GetMapping("/stream/{symbol}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamSymbol(@PathVariable symbol: String): SseEmitter {
        totalConnections.increment()
        activeConnections.incrementAndGet()

        val emitter = SseEmitter(Long.MAX_VALUE)

        val subscription = priceGenerator.subscribeToSymbol(symbol.uppercase())
            .doOnNext { price ->
                try {
                    emitter.send(
                        SseEmitter.event()
                            .data(price)
                            .id("${price.symbol}-${price.timestamp.toEpochMilli()}")
                    )
                } catch (e: Exception) {
                    emitter.completeWithError(e)
                }
            }
            .doOnCancel { cleanup(emitter) }
            .doOnComplete { cleanup(null) }
            .doOnError { cleanup(null) }
            .subscribe()

        emitter.onCompletion {
            subscription.dispose()
            cleanup(null)
        }

        emitter.onTimeout {
            subscription.dispose()
            cleanup(null)
        }

        return emitter
    }

    private fun cleanup(emitter: SseEmitter?) {
        activeConnections.decrementAndGet()
        emitter?.complete()
    }
}
