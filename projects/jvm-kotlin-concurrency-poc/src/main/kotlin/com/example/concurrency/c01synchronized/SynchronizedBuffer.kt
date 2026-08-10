package com.example.concurrency.c01synchronized

import com.example.concurrency.common.ItemBuffer
import com.example.concurrency.common.Scenario
import com.example.concurrency.common.runThreaded
import java.util.ArrayDeque

/**
 * 1번: 가장 원초적인 방법. 모니터 락(synchronized) + wait/notifyAll 로
 * bounded buffer 를 직접 구현한다. BlockingQueue 내부도 결국 이 원리.
 *
 * Kotlin 메모:
 *  - Java 의 `synchronized` 키워드가 Kotlin 엔 없다.
 *  - 대신 (a) 메서드에 @Synchronized 어노테이션, (b) synchronized(lock) {} 인라인 함수.
 *  - 여기선 인라인 함수 형태(synchronized(lock){})를 쓴다 — Java 의 synchronized(this){} 와 동일 바이트코드.
 *
 * 핵심 함정:
 *  - wait() 는 반드시 while 루프 안에서 조건 재검사 (spurious wakeup + 다중 consumer 경쟁).
 *    if 로 쓰면 깨어났을 때 조건이 이미 깨져 버그.
 *  - notify() 대신 notifyAll() — producer/consumer 가 같은 모니터에서 대기하므로
 *    notify() 는 엉뚱한 쪽만 깨워 교착 위험.
 */
class SynchronizedBuffer(private val capacity: Int) : ItemBuffer {
    private val lock = Any()
    private val queue = ArrayDeque<Long>(capacity)

    override fun put(value: Long) {
        synchronized(lock) {
            while (queue.size == capacity) {
                (lock as Object).wait() // 가득 참 → 자리 날 때까지 대기
            }
            queue.addLast(value)
            (lock as Object).notifyAll() // 비어있던 consumer 깨우기
        }
    }

    override fun take(): Long {
        synchronized(lock) {
            while (queue.isEmpty()) {
                (lock as Object).wait() // 비어있음 → 들어올 때까지 대기
            }
            val v = queue.removeFirst()
            (lock as Object).notifyAll() // 가득 찼던 producer 깨우기
            return v
        }
    }
}

fun main() {
    val scenario = Scenario()
    val buffer = SynchronizedBuffer(capacity = 1024)
    runThreaded("synchronized", scenario, buffer).print()
}
