package com.example.sse.service

import com.example.sse.domain.StockPrice
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Gauge
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

@Service
class PriceGenerator(private val meterRegistry: MeterRegistry) {

    private val sink = Sinks.many().multicast().onBackpressureBuffer<StockPrice>()
    private val prices = ConcurrentHashMap<String, BigDecimal>()
    private val subscriberCount = AtomicLong(0)

    private val symbols = listOf(
        "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA",
        "META", "NVDA", "AMD", "INTC", "NFLX",
        "JPM", "BAC", "WMT", "DIS", "PYPL"
    )

    private val generatedCounter: Counter
    private val subscriberGauge: Gauge

    init {
        // Initialize prices
        symbols.forEach { symbol ->
            prices[symbol] = BigDecimal.valueOf(Random.nextDouble(100.0, 1000.0))
                .setScale(2, RoundingMode.HALF_UP)
        }

        // Metrics
        generatedCounter = Counter.builder("sse.prices.generated")
            .description("Total number of stock prices generated")
            .register(meterRegistry)

        subscriberGauge = Gauge.builder("sse.subscribers.active", subscriberCount, { it.get().toDouble() })
            .description("Current number of active SSE subscribers")
            .register(meterRegistry)
    }

    @PostConstruct
    fun startGeneration() {
        Flux.interval(Duration.ofSeconds(1))
            .doOnNext { generatePrices() }
            .subscribe()
    }

    private fun generatePrices() {
        symbols.forEach { symbol ->
            val currentPrice = prices[symbol]!!
            val changePercent = BigDecimal.valueOf(Random.nextDouble(-5.0, 5.0))
                .setScale(2, RoundingMode.HALF_UP)
            val change = currentPrice.multiply(changePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            val newPrice = currentPrice.add(change).setScale(2, RoundingMode.HALF_UP)

            prices[symbol] = newPrice

            val stockPrice = StockPrice(
                symbol = symbol,
                price = newPrice,
                change = change,
                changePercent = changePercent,
                volume = Random.nextLong(10000, 1000000),
                timestamp = Instant.now()
            )

            sink.tryEmitNext(stockPrice)
            generatedCounter.increment()
        }
    }

    fun subscribeToAll(): Flux<StockPrice> {
        subscriberCount.incrementAndGet()
        return sink.asFlux()
            .doOnCancel { subscriberCount.decrementAndGet() }
            .doOnTerminate { subscriberCount.decrementAndGet() }
    }

    fun subscribeToSymbol(symbol: String): Flux<StockPrice> {
        subscriberCount.incrementAndGet()
        return sink.asFlux()
            .filter { it.symbol == symbol }
            .doOnCancel { subscriberCount.decrementAndGet() }
            .doOnTerminate { subscriberCount.decrementAndGet() }
    }

    @PreDestroy
    fun cleanup() {
        sink.tryEmitComplete()
    }
}
