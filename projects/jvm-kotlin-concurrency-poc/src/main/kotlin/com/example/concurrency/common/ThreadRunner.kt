package com.example.concurrency.common

import kotlin.concurrent.thread

/**
 * 스레드 기반 예제(1~4번)가 공유하는 buffer 계약.
 * put/take 는 블로킹일 수도 있고(BlockingQueue) 직접 wait/notify 일 수도 있다.
 */
interface ItemBuffer {
    fun put(value: Long)
    fun take(): Long
}

/** consumer 종료 신호용 독약(poison pill). 실제 아이템 값은 항상 >= 1 이므로 충돌 없음. */
const val POISON: Long = -1L

/**
 * N producer × M consumer 를 진짜 OS 스레드로 돌린다.
 * 모든 producer 가 끝나면 consumer 수만큼 POISON 을 넣어 깔끔히 종료시킨다.
 *
 * 이 runner 는 "락/큐를 어떻게 구현했나"와 무관 — buffer 만 갈아끼우면 됨.
 * 즉 1~4번 예제의 차이는 오직 ItemBuffer 구현체뿐이다.
 */
fun runThreaded(name: String, scenario: Scenario, buffer: ItemBuffer): Result {
    val cs = Checksums()

    val elapsed = timed {
        val consumers = (1..scenario.consumers).map {
            thread(name = "consumer-$it") {
                while (true) {
                    val v = buffer.take()
                    if (v == POISON) break
                    cs.onConsume(v)
                }
            }
        }

        val producers = (1..scenario.producers).map { p ->
            thread(name = "producer-$p") {
                // producer 마다 겹치지 않는 값 구간을 써서 checksum 이 유의미하게.
                val base = (p - 1).toLong() * scenario.itemsPerProducer
                for (i in 1..scenario.itemsPerProducer) {
                    val v = base + i // 항상 >= 1
                    cs.onProduce(v)
                    buffer.put(v)
                }
            }
        }

        producers.forEach { it.join() }
        // 생산 끝 → consumer 마다 독약 1개씩.
        repeat(scenario.consumers) { buffer.put(POISON) }
        consumers.forEach { it.join() }
    }

    return cs.toResult(name, scenario, elapsed)
}
