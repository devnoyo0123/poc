package com.example.concurrency.c06mutexflow

import com.example.concurrency.common.Checksums
import com.example.concurrency.common.Scenario
import com.example.concurrency.common.timed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 6번: 코루틴 동기화 도구 Mutex + 스트림 Flow.
 *
 * (A) Mutex = "코루틴용 ReentrantLock". 결정적 차이:
 *     - ReentrantLock.lock() 은 대기 시 OS 스레드를 블로킹.
 *     - Mutex.lock() 은 대기 시 코루틴만 서스펜드 (스레드는 반납).
 *     - 그래서 코루틴 안에서 공유 가변 상태 보호엔 ReentrantLock 말고 Mutex.
 *     - 주의: Mutex 는 재진입(reentrant) 아님! 같은 코루틴이 두 번 lock 하면 교착.
 *       (ReentrantLock 은 재진입 허용 — 이름 그대로.)
 *
 * (B) Flow = 콜드 비동기 스트림. .buffer() 로 producer-consumer backpressure 표현.
 *     Flow 는 본질적으로 단일 소비자(collect 1곳)용. 다중 소비자 팬아웃이 필요하면
 *     Channel(5번) 또는 SharedFlow 를 쓴다. 여기선 N producer → 1 collector.
 */

/** (A) Mutex 로 공유 카운터 보호 — race 없는지 checksum 으로 증명. */
fun mutexDemo(): String {
    val mutex = Mutex()
    var shared = 0L // 의도적으로 비원자 가변 상태
    val workers = 16
    val incsEach = 100_000
    runBlocking(Dispatchers.Default) {
        repeat(workers) {
            launch {
                repeat(incsEach) {
                    mutex.withLock { shared++ } // withLock = lock/unlock try-finally
                }
            }
        }
    }
    val expected = workers.toLong() * incsEach
    val ok = if (shared == expected) "OK" else "FAIL"
    return "[$ok] Mutex 카운터: $shared (기대 $expected)"
}

/** (B) Flow 파이프라인을 producer-consumer 로. N producer → buffer → 1 collector. */
fun runFlow(scenario: Scenario): Pair<Checksums, Long> {
    val cs = Checksums()
    val elapsed = timed {
        runBlocking(Dispatchers.Default) {
            // channelFlow: 여러 코루틴이 동시에 send 가능한 Flow 빌더.
            val stream = channelFlow {
                (1..scenario.producers).map { p ->
                    launch {
                        val base = (p - 1).toLong() * scenario.itemsPerProducer
                        for (i in 1..scenario.itemsPerProducer) {
                            val v = base + i
                            cs.onProduce(v)
                            send(v) // 버퍼 차면 서스펜드(backpressure)
                        }
                    }
                }
                // channelFlow 블록은 자식 코루틴 끝나면 자동으로 채널 닫음(구조적 동시성).
            }.buffer(1024, onBufferOverflow = BufferOverflow.SUSPEND)
                .flowOn(Dispatchers.Default)

            // 단일 collector = consumer.
            stream.collect { v -> cs.onConsume(v) }
        }
    }
    return cs to elapsed
}

fun main() {
    println(mutexDemo())
    val scenario = Scenario()
    val (cs, elapsed) = runFlow(scenario)
    // Flow 는 단일 consumer 이므로 consumers=1 로 본 결과.
    cs.toResult("flow(1 consumer)", scenario.copy(consumers = 1), elapsed).print()
}
