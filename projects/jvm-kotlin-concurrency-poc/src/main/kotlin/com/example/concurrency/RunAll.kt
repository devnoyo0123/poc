package com.example.concurrency

import com.example.concurrency.c01synchronized.SynchronizedBuffer
import com.example.concurrency.c02reentrantlock.ReentrantLockBuffer
import com.example.concurrency.c03blockingqueue.BlockingQueueBuffer
import com.example.concurrency.c05channel.runChannel
import com.example.concurrency.c06mutexflow.mutexDemo
import com.example.concurrency.c06mutexflow.runFlow
import com.example.concurrency.common.Scenario
import com.example.concurrency.common.runThreaded

/**
 * 전체 비교 러너. 같은 시나리오를 스레드 기반 1~3번 + 코루틴 기반 5~6번으로 돌려
 * 정확성(OK/FAIL)과 처리량을 한 표로 비교.
 *
 * 4번 ConcurrentBag 은 자원 풀이라 형태가 달라 별도 main:
 *   ./gradlew runExample -PmainClass=com.example.concurrency.c04concurrentbag.ConcurrentBagBenchKt
 */
fun main() {
    val scenario = Scenario(producers = 4, consumers = 4, itemsPerProducer = 25_000)
    println("시나리오: producers=${scenario.producers} consumers=${scenario.consumers} total=${"%,d".format(scenario.totalItems)}")
    println("기대 checksum = N(N+1)/2 = ${"%,d".format(scenario.totalItems.toLong() * (scenario.totalItems + 1) / 2)}")
    println("-".repeat(96))

    // 워밍업 (JIT 최적화 유도 — 첫 실행은 항상 느림).
    runThreaded("warmup", scenario.copy(itemsPerProducer = 2000), BlockingQueueBuffer(256))

    // --- 스레드 기반 (블로킹) ---
    runThreaded("1.synchronized", scenario, SynchronizedBuffer(1024)).print()
    runThreaded("2.reentrantlock", scenario, ReentrantLockBuffer(1024)).print()
    runThreaded("3.blockingqueue", scenario, BlockingQueueBuffer(1024)).print()

    // --- 코루틴 기반 (서스펜드) ---
    val (chCs, chNs) = runChannel(scenario)
    chCs.toResult("5.channel", scenario, chNs).print()

    val (flCs, flNs) = runFlow(scenario)
    flCs.toResult("6.flow(1cons)", scenario.copy(consumers = 1), flNs).print()

    println("-".repeat(96))
    println(mutexDemo())
    println()
    println("참고: 처리량 절대값은 머신/JIT/GC 영향 큼. 상대 비교 + 정확성(OK) 위주로 볼 것.")
    println("4.ConcurrentBag 벤치는 별도: ./gradlew runExample -PmainClass=com.example.concurrency.c04concurrentbag.ConcurrentBagBenchKt")
}
