package com.example.concurrency.c04concurrentbag

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 4번: HikariCP 의 ConcurrentBag 미니 재구현 (핵심 메커니즘만, 충실하게).
 *
 * ⚠️ 오해 정정: .NET 의 ConcurrentBag 과 다른 물건이다.
 *   - 또한 producer-consumer "파이프"가 아니다. **재사용 자원 풀**(커넥션 풀)이다.
 *     아이템을 borrow → 사용 → requite(반납) 하고, 같은 아이템이 계속 돌고 돈다.
 *   - 그래서 1~3번의 ItemBuffer(put/take 단방향)에 끼워맞추지 않고 별도 벤치를 둔다.
 *
 * 왜 빠른가 (lock-free 설계 3층):
 *   1) ThreadLocal 캐시(threadList): 내가 방금 쓴 자원을 스레드 로컬에 기억.
 *      borrow 시 여기부터 봄 → 경쟁(contention) 0, 락 0. HikariCP 속도의 핵심.
 *   2) 공유 리스트(sharedList) steal: 내 로컬에 없으면 공유 리스트를 훑어
 *      CAS(NOT_IN_USE→IN_USE)로 남의 자원을 가로챔(steal). 락 대신 CAS.
 *   3) handoff(SynchronousQueue): 그래도 없으면 대기. 반납자가 직접 건네줌.
 *
 * 상태 전이는 전부 AtomicInteger.compareAndSet — 모니터 락 없음.
 */
class ConcurrentBag<T : ConcurrentBag.BagEntry> {

    companion object {
        const val NOT_IN_USE = 0
        const val IN_USE = 1
        const val REMOVED = -1
    }

    /** 풀에 담기는 자원은 이걸 상속. 상태를 원자적으로 들고 다닌다. */
    abstract class BagEntry {
        private val state = AtomicInteger(NOT_IN_USE)
        fun compareAndSet(expect: Int, update: Int) = state.compareAndSet(expect, update)
        fun setState(update: Int) = state.set(update)
        fun getState() = state.get()
    }

    private val sharedList = CopyOnWriteArrayList<T>()
    // ThreadLocal: 스레드별 "최근 쓴 자원" 캐시. weak 참조까진 생략(미니 버전).
    private val threadList = ThreadLocal.withInitial { ArrayList<T>(16) }
    private val waiters = AtomicInteger()
    private val handoffQueue = SynchronousQueue<T>(true)

    fun add(entry: T) {
        sharedList.add(entry)
        // 대기자 있으면 바로 건네주기 시도.
        while (waiters.get() > 0 && entry.getState() == NOT_IN_USE && !handoffQueue.offer(entry)) {
            Thread.yield()
        }
    }

    /** 자원 빌리기. 못 빌리면 timeout 후 null. */
    fun borrow(timeout: Long, unit: TimeUnit): T? {
        // 1층: 내 ThreadLocal 캐시 (역순 — 가장 최근 반납분부터, 캐시 친화적)
        // 미니 버전 메모: 적중 시 리스트에서 제거한다. 본가 HikariCP 는 WeakReference 로
        // 남겨두지만, 여기선 제거해 캐시를 풀 크기 수준으로 묶어 무한 증가를 막는다.
        val list = threadList.get()
        for (i in list.indices.reversed()) {
            val e = list[i]
            if (e.compareAndSet(NOT_IN_USE, IN_USE)) {
                list.removeAt(i)
                return e
            }
        }

        // 2층 & 3층: 공유 리스트 steal, 실패 시 handoff 대기.
        waiters.incrementAndGet()
        try {
            for (e in sharedList) {
                if (e.compareAndSet(NOT_IN_USE, IN_USE)) {
                    return e
                }
            }
            var remaining = unit.toNanos(timeout)
            while (remaining > 0L) {
                val start = System.nanoTime()
                val e = handoffQueue.poll(remaining, TimeUnit.NANOSECONDS) ?: return null
                if (e.compareAndSet(NOT_IN_USE, IN_USE)) return e
                remaining -= System.nanoTime() - start
            }
            return null
        } finally {
            waiters.decrementAndGet()
        }
    }

    /** 자원 반납. 대기자 있으면 직접 건네고, 없으면 내 ThreadLocal 에 캐시. */
    fun requite(entry: T) {
        entry.setState(NOT_IN_USE)
        // 대기자에게 직접 handoff 시도.
        repeat(waiters.get()) {
            if (entry.getState() != NOT_IN_USE || handoffQueue.offer(entry)) return
            Thread.yield()
        }
        // 대기자 없음 → 내 스레드 로컬에 캐시 (다음 borrow 가 1층에서 즉시 회수).
        threadList.get().add(entry)
    }

    fun size() = sharedList.size
}
