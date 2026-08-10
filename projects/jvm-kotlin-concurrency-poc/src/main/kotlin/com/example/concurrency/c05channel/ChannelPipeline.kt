package com.example.concurrency.c05channel

import com.example.concurrency.common.Checksums
import com.example.concurrency.common.Scenario
import com.example.concurrency.common.timed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 5번: Kotlin Channel. producer-consumer 의 코루틴 버전.
 *
 * 1~4번과 결정적 차이 — 여기엔 OS 스레드가 producer/consumer 수만큼 없다.
 *  - Channel.send() 가 가득 차면 스레드를 블로킹하지 않고 코루틴을 "서스펜드"한다.
 *    스레드는 반납되어 다른 코루틴을 실행 → 수천 producer 도 스레드 몇 개로 감당.
 *  - Channel 은 본질적으로 "코루틴용 BlockingQueue". capacity 로 backpressure 조절.
 *
 * Channel capacity 종류:
 *  - RENDEZVOUS(0, 기본): SynchronousQueue 처럼 send-receive 직접 만남.
 *  - 고정 N: ArrayBlockingQueue 처럼 버퍼 N.
 *  - UNLIMITED / CONFLATED / BUFFERED 도 있음.
 *
 * "코루틴은 syntax sugar 아님"의 증거: suspend 함수는 컴파일러가 상태머신(CPS)으로
 * 변환한다. send/receive 지점이 상태 분기점. Java 엔 없는 진짜 기능.
 */
fun runChannel(scenario: Scenario): Pair<Checksums, Long> {
    val cs = Checksums()
    val elapsed = timed {
        runBlocking(Dispatchers.Default) {
            // capacity = 1024 → 3번 ArrayBlockingQueue(1024) 와 동일 backpressure.
            val channel = Channel<Long>(capacity = 1024)

            coroutineScope {
                // consumer 코루틴 M개
                val consumers = (1..scenario.consumers).map {
                    launch {
                        for (v in channel) { // 채널 닫힐 때까지 receive 반복
                            cs.onConsume(v)
                        }
                    }
                }

                // producer 코루틴 N개
                val producers = (1..scenario.producers).map { p ->
                    launch {
                        val base = (p - 1).toLong() * scenario.itemsPerProducer
                        for (i in 1..scenario.itemsPerProducer) {
                            val v = base + i
                            cs.onProduce(v)
                            channel.send(v) // 가득 차면 스레드 블로킹 X, 코루틴 서스펜드 O
                        }
                    }
                }

                // 구조적 동시성: producer 전부 끝나길 기다린 뒤 채널 닫음.
                // → consumer 의 for 루프가 자연 종료. 독약(POISON) 불필요!
                producers.forEach { it.join() }
                channel.close()
                consumers.forEach { it.join() }
            }
        }
    }
    return cs to elapsed
}

fun main() {
    val scenario = Scenario()
    val (cs, elapsed) = runChannel(scenario)
    cs.toResult("channel", scenario, elapsed).print()
}
