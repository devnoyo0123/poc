package com.example.concurrency.common

import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime

/**
 * 모든 예제가 공유하는 producer-consumer 시나리오 정의 + 검증 도구.
 *
 * 목적: 같은 작업량을 6가지 동시성 방식으로 처리하고
 *       (1) 정확성(생산 == 소비, 유실/중복 0) (2) 처리량(items/sec) 을 비교.
 */
data class Scenario(
    val producers: Int = 4,
    val consumers: Int = 4,
    val itemsPerProducer: Int = 25_000,
) {
    val totalItems: Int get() = producers * itemsPerProducer
}

data class Result(
    val name: String,
    val scenario: Scenario,
    val produced: Long,
    val consumed: Long,
    val checksumProduced: Long,
    val checksumConsumed: Long,
    val elapsedNanos: Long,
) {
    val correct: Boolean
        get() = produced == scenario.totalItems.toLong() &&
            consumed == produced &&
            checksumProduced == checksumConsumed

    val throughputPerSec: Double
        get() = consumed.toDouble() / (elapsedNanos / 1_000_000_000.0)

    fun print() {
        val ms = elapsedNanos / 1_000_000.0
        val ok = if (correct) "OK " else "FAIL"
        println(
            "[%-4s] %-22s | %,d items | %7.1f ms | %,.0f items/s | sumP=%d sumC=%d"
                .format(ok, name, consumed, ms, throughputPerSec, checksumProduced, checksumConsumed)
        )
    }
}

/**
 * 검증 트릭: 각 아이템에 1..N 정수 값을 부여하고 producer/consumer 양쪽에서
 * 합(checksum)을 누적한다. 두 합이 같으면 유실·중복이 없었다는 강한 증거.
 * 단순 카운트만 비교하면 "하나 잃고 하나 중복" 같은 버그를 놓침.
 */
class Checksums {
    val producedCount = AtomicLong()
    val consumedCount = AtomicLong()
    val producedSum = AtomicLong()
    val consumedSum = AtomicLong()

    fun onProduce(value: Long) {
        producedCount.incrementAndGet()
        producedSum.addAndGet(value)
    }

    fun onConsume(value: Long) {
        consumedCount.incrementAndGet()
        consumedSum.addAndGet(value)
    }

    fun toResult(name: String, scenario: Scenario, elapsedNanos: Long) = Result(
        name = name,
        scenario = scenario,
        produced = producedCount.get(),
        consumed = consumedCount.get(),
        checksumProduced = producedSum.get(),
        checksumConsumed = consumedSum.get(),
        elapsedNanos = elapsedNanos,
    )
}

/** 측정 보일러플레이트 제거용. block 안에서 실제 producer/consumer 를 돌린다. */
inline fun timed(block: () -> Unit): Long = measureNanoTime(block)
