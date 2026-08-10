package com.example.concurrency.c02reentrantlock

import com.example.concurrency.common.ItemBuffer
import com.example.concurrency.common.Scenario
import com.example.concurrency.common.runThreaded
import java.util.ArrayDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 2번: ReentrantLock + Condition. synchronized 보다 명시적이고 강력.
 *
 * synchronized 대비 장점:
 *  - Condition 을 여러 개 둘 수 있다 → notFull / notEmpty 분리.
 *    producer 는 notEmpty 만, consumer 는 notFull 만 깨움 → notifyAll 의 "헛깨움" 제거.
 *  - tryLock(timeout), lockInterruptibly, fair 모드(생성자 ReentrantLock(true)) 지원.
 *  - 재진입(reentrant): 같은 스레드가 이미 쥔 락을 또 lock() 해도 안 막힘(holdCount++).
 *
 * Kotlin 메모: kotlin.concurrent.withLock { } 확장이 lock()/unlock() try-finally 를 대신.
 *             Java 의 lock(); try {...} finally { unlock() } 보일러플레이트 사라짐.
 */
class ReentrantLockBuffer(private val capacity: Int) : ItemBuffer {
    private val lock = ReentrantLock()
    private val notFull = lock.newCondition()
    private val notEmpty = lock.newCondition()
    private val queue = ArrayDeque<Long>(capacity)

    override fun put(value: Long) {
        lock.withLock {
            while (queue.size == capacity) {
                notFull.await() // 자리 날 때까지
            }
            queue.addLast(value)
            notEmpty.signal() // consumer 정확히 깨움 (signalAll 불필요)
        }
    }

    override fun take(): Long {
        lock.withLock {
            while (queue.isEmpty()) {
                notEmpty.await()
            }
            val v = queue.removeFirst()
            notFull.signal() // producer 정확히 깨움
            return v
        }
    }
}

fun main() {
    val scenario = Scenario()
    val buffer = ReentrantLockBuffer(capacity = 1024)
    runThreaded("reentrantlock", scenario, buffer).print()
}
