package com.example.concurrency.c03blockingqueue

import com.example.concurrency.common.ItemBuffer
import com.example.concurrency.common.Scenario
import com.example.concurrency.common.runThreaded
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * 3번: BlockingQueue. 1~2번을 직접 만들 필요 없이 표준 라이브러리가 제공.
 * 실무에서 producer-consumer 면 거의 항상 이걸 씀.
 *
 * 내부 구현(ArrayBlockingQueue)이 바로 2번 패턴 — ReentrantLock + notFull/notEmpty Condition.
 * 즉 2번을 손으로 만든 이유는 "이 안에서 무슨 일이 일어나는지" 보기 위함.
 *
 * 종류 감 잡기:
 *  - ArrayBlockingQueue   : 고정 크기 배열, 단일 락. 가장 예측 가능. (여기 사용)
 *  - LinkedBlockingQueue  : 링크드, put/take 락 분리(two-lock) → 고경쟁시 처리량 ↑.
 *  - SynchronousQueue     : 버퍼 0. put 은 take 가 받을 때까지 직접 전달(handoff).
 *  - LinkedTransferQueue  : SynchronousQueue + 큐 혼합, 가장 진보적.
 *
 * put()/take() 는 블로킹. offer()/poll() 은 즉시 반환(논블로킹) 또는 timeout 버전 존재.
 */
class BlockingQueueBuffer(capacity: Int) : ItemBuffer {
    private val queue: BlockingQueue<Long> = ArrayBlockingQueue(capacity)
    override fun put(value: Long) = queue.put(value)   // 가득 차면 블로킹
    override fun take(): Long = queue.take()            // 비면 블로킹
}

fun main() {
    val scenario = Scenario()
    val buffer = BlockingQueueBuffer(capacity = 1024)
    runThreaded("blockingqueue", scenario, buffer).print()
}
