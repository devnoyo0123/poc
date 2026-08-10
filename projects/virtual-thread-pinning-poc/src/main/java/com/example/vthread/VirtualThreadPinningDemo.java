package com.example.vthread;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class VirtualThreadPinningDemo {

    private static final Object MONITOR = new Object();
    private static final ReentrantLock REENTRANT_LOCK = new ReentrantLock();
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    private static final InheritableThreadLocal<String> INHERITED_REQUEST_ID = new InheritableThreadLocal<>();

    public static void main(String[] args) throws Exception {
        printGuideline();
        reentrantLockBasics();
        tryLockAvoidsWaitingForever();
        threadLocalWorksButDoesNotMagicallyPropagate();
        countDownLatchWorksOnVirtualThread();
        synchronizedPinsCarrier();
        reentrantLockDoesNotPinCarrier();
    }

    private static void printGuideline() {
        System.out.println("""

                === When to use virtual threads ===
                Use:
                - blocking MVC/JPA/JDBC/HTTP client code
                - many concurrent I/O waits
                - thread-per-request code you do not want to rewrite to reactive

                Avoid / watch:
                - CPU-bound work expecting speedup
                - synchronized around DB/API/blocking calls
                - native/JNI/foreign calls that block for long
                - unlimited downstream pressure: DB pool/API rate limits still matter

                ReentrantLock note:
                - ReentrantLock is virtual-thread-friendly for waiting.
                - It does not magically remove lock contention.
                - Keep blocking I/O outside locks when possible.

                ThreadLocal note:
                - Virtual threads are still Java Thread instances.
                - ThreadLocal works.
                - Values do not automatically propagate to child/new virtual threads.
                - Clear ThreadLocal values to avoid per-thread memory retention.
                """);
    }

    private static void reentrantLockBasics() {
        System.out.println("\n=== ReentrantLock basics: same thread can re-enter ===");

        REENTRANT_LOCK.lock();
        try {
            log("first lock acquired. holdCount=" + REENTRANT_LOCK.getHoldCount());
            nestedLock();
        } finally {
            log("unlock first lock. holdCount before unlock=" + REENTRANT_LOCK.getHoldCount());
            REENTRANT_LOCK.unlock();
        }
    }

    private static void nestedLock() {
        REENTRANT_LOCK.lock();
        try {
            log("same thread re-entered lock. holdCount=" + REENTRANT_LOCK.getHoldCount());
        } finally {
            log("unlock nested lock. holdCount before unlock=" + REENTRANT_LOCK.getHoldCount());
            REENTRANT_LOCK.unlock();
        }
    }

    private static void tryLockAvoidsWaitingForever() throws Exception {
        System.out.println("\n=== ReentrantLock tryLock: fail instead of waiting forever ===");

        CountDownLatch lockHeld = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                REENTRANT_LOCK.lock();
                try {
                    log("holder acquired lock for 1000ms");
                    lockHeld.countDown();
                    sleep(1000);
                } finally {
                    REENTRANT_LOCK.unlock();
                    log("holder released lock");
                }
            });

            lockHeld.await();

            executor.submit(() -> {
                try {
                    boolean acquired = REENTRANT_LOCK.tryLock(100, TimeUnit.MILLISECONDS);
                    if (!acquired) {
                        log("tryLock timed out. caller can fallback instead of blocking forever");
                        return;
                    }

                    try {
                        log("tryLock acquired lock");
                    } finally {
                        REENTRANT_LOCK.unlock();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }).get();
        }
    }

    private static void threadLocalWorksButDoesNotMagicallyPropagate() throws Exception {
        System.out.println("\n=== ThreadLocal on virtual threads ===");

        REQUEST_ID.set("request-123");
        INHERITED_REQUEST_ID.set("inherited-request-456");

        try {
            log("main ThreadLocal=" + REQUEST_ID.get());

            Thread virtualThread = Thread.startVirtualThread(() -> {
                log("new virtual thread ThreadLocal=" + REQUEST_ID.get());
                log("new virtual thread InheritableThreadLocal=" + INHERITED_REQUEST_ID.get());

                REQUEST_ID.set("virtual-request-789");
                log("new virtual thread local value after set=" + REQUEST_ID.get());
                REQUEST_ID.remove();
            });

            virtualThread.join();
            log("main ThreadLocal after child finished=" + REQUEST_ID.get());
        } finally {
            REQUEST_ID.remove();
            INHERITED_REQUEST_ID.remove();
        }
    }

    private static void countDownLatchWorksOnVirtualThread() throws Exception {
        System.out.println("\n=== CountDownLatch works on virtual threads ===");

        CountDownLatch latch = new CountDownLatch(1);

        Thread.startVirtualThread(() -> {
            log("virtual thread countDown()");
            latch.countDown();
        });

        latch.await();
        log("main passed latch.await()");
    }

    private static void synchronizedPinsCarrier() throws Exception {
        System.out.println("\n=== synchronized + blocking pins carrier ===");
        System.out.println("Expected: unrelated virtual thread is delayed when carrier parallelism = 1.");

        CountDownLatch enteredMonitor = new CountDownLatch(1);
        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                synchronized (MONITOR) {
                    logSince(start, "entered synchronized block, now sleep 1500ms", 0);
                    enteredMonitor.countDown();
                    sleep(1500);
                    logSince(start, "leaving synchronized block", 0);
                }
            });

            enteredMonitor.await();

            executor.submit(() -> {
                logSince(start, "unrelated virtual thread ran", 0);
            }).get();
        }
    }

    private static void reentrantLockDoesNotPinCarrier() throws Exception {
        System.out.println("\n=== ReentrantLock + blocking does not pin carrier ===");
        System.out.println("Expected: unrelated virtual thread runs quickly even while lock holder sleeps.");

        CountDownLatch enteredLock = new CountDownLatch(1);
        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                REENTRANT_LOCK.lock();
                try {
                    logSince(start, "entered ReentrantLock, now sleep 1500ms", 0);
                    enteredLock.countDown();
                    sleep(1500);
                    logSince(start, "leaving ReentrantLock", 0);
                } finally {
                    REENTRANT_LOCK.unlock();
                }
            });

            enteredLock.await();

            executor.submit(() -> {
                logSince(start, "unrelated virtual thread ran", 0);
            }).get();
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static void log(String message) {
        System.out.printf("[%s] %s%n", Thread.currentThread(), message);
    }

    private static void logSince(Instant start, String message, long ignored) {
        long elapsedMillis = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("%4dms [%s] %s%n", elapsedMillis, Thread.currentThread(), message);
    }
}
