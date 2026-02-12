package com.example.sse.domain

import java.math.BigDecimal
import java.time.Instant

data class StockPrice(
    val symbol: String,
    val price: BigDecimal,
    val change: BigDecimal,
    val changePercent: BigDecimal,
    val volume: Long,
    val timestamp: Instant
)
