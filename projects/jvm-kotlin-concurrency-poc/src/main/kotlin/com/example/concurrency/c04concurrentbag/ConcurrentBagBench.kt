package com.example.concurrency.c04concurrentbag

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.system.measureNanoTime

/**
 * ConcurrentBag 벤치: 재사용 자원 풀 시나리오.
 * 풀에 자원 K개, 스레드 N개가 각자 borrow→(짧은 작업)→requite 를 R번 반복.
 *
 * 비교군: 같은 풀을 ArrayBlockingQueue 로 구현(take=borrow, put=requite).
 * 관전 포인트: 스레드가 자기 자원을 반복 재사용할 때 ConcurrentBag 의
 *            ThreadLocal 1층 적중률이 높아 락 큐 대비 경쟁이 적다.
 */

/**
 * live: 이 자원을 지금 몇 개 스레드가 쥐고 있나. 정상이면 항상 0→1→0.
 * borrow 직후 0→1 CAS 가 실패하면 = 다른 스레드가 동시에 같은 자원을 쥠 = 상호배제 깨짐.
 */
private class PooledResource(val id: Int) : ConcurrentBag.BagEntry() {
    val live = AtomicLong(0)
}

/** 풀의 올바름 = 처리량이 아니라 "한 자원을 둘이 동시에 쓰지 않음"(상호배제). */
private fun useExclusively(r: PooledResource, corruption: AtomicLong) {
    if (!r.live.compareAndSet(0, 1)) corruption.incrementAndGet() // 동시 점유 감지
    // "작업" 흉내 — 점유 구간.
    if (!r.live.compareAndSet(1, 0)) corruption.incrementAndGet()
}

private fun benchConcurrentBag(threads: Int, poolSize: Int, rounds: Int): Pair<Long, Long> {
    val bag = ConcurrentBag<PooledResource>()
    repeat(poolSize) { bag.add(PooledResource(it)) }
    val corruption = AtomicLong()

    val nanos = measureNanoTime {
        (1..threads).map {
            thread {
                repeat(rounds) {
                    val r = bag.borrow(5, TimeUnit.SECONDS)
                        ?: error("borrow timeout — 풀 고갈/버그")
                    useExclusively(r, corruption)
                    bag.requite(r)
                }
            }
        }.forEach { it.join() }
    }
    return nanos to corruption.get()
}

private fun benchBlockingQueuePool(threads: Int, poolSize: Int, rounds: Int): Pair<Long, Long> {
    val pool = ArrayBlockingQueue<PooledResource>(poolSize)
    repeat(poolSize) { pool.put(PooledResource(it)) }
    val corruption = AtomicLong()

    val nanos = measureNanoTime {
        (1..threads).map {
            thread {
                repeat(rounds) {
                    val r = pool.take()            // borrow
                    useExclusively(r, corruption)
                    pool.put(r)                    // requite
                }
            }
        }.forEach { it.join() }
    }
    return nanos to corruption.get()
}

fun main() {
    val threads = 8
    val poolSize = 8
    val rounds = 200_000

    // 워밍업 (JIT) — 측정 전 한 번 돌려 코드 최적화 유도.
    benchConcurrentBag(threads, poolSize, 10_000)
    benchBlockingQueuePool(threads, poolSize, 10_000)

    val (bagNs, bagCorrupt) = benchConcurrentBag(threads, poolSize, rounds)
    val (bqNs, bqCorrupt) = benchBlockingQueuePool(threads, poolSize, rounds)

    val totalOps = threads.toLong() * rounds
    fun line(name: String, ns: Long, corrupt: Long) {
        val ms = ns / 1_000_000.0
        val ops = totalOps / (ns / 1_000_000_000.0)
        val ok = if (corrupt == 0L) "OK" else "FAIL"
        println("[%-4s] %-22s | %,d borrow/requite | %7.1f ms | %,.0f ops/s | 상호배제위반=%d".format(ok, name, totalOps, ms, ops, corrupt))
    }
    println("== ConcurrentBag vs BlockingQueue 풀 (threads=$threads pool=$poolSize) ==")
    line("ConcurrentBag", bagNs, bagCorrupt)
    line("ArrayBlockingQueue pool", bqNs, bqCorrupt)
    require(bagCorrupt == 0L && bqCorrupt == 0L) { "상호배제 위반 — 풀 버그" }
    println()
    println("해석: 작은 풀/낮은 경쟁에선 단일 락 ArrayBlockingQueue 가 더 빠를 수 있다.")
    println("     ConcurrentBag 의 ThreadLocal+steal 설계는 '스레드 수↑ + 자기 자원 재사용'")
    println("     패턴(=실제 커넥션 풀)에서 단일 락 경쟁을 회피할 때 빛난다. 만능 아님.")
}
