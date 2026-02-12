package com.example.sse.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PostConstruct

@Configuration
class MetricsConfig(private val meterRegistry: MeterRegistry) {

    @PostConstruct
    fun bindMetrics() {
        // JVM Memory Metrics
        JvmMemoryMetrics().bindTo(meterRegistry)

        // JVM Thread Metrics (Critical for comparing thread usage!)
        JvmThreadMetrics().bindTo(meterRegistry)

        // CPU Metrics
        ProcessorMetrics().bindTo(meterRegistry)
    }
}
