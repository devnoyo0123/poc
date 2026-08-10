package com.example.vthread.quiz;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Quiz:
 *
 * 1. Thread-A가 updateOrder() 실행 중일 때 Thread-B가 cancelOrder() 호출하면 바로 실행될까, 대기할까?
 * 2. Thread-A가 updateOrder() 실행 중일 때 Thread-C가 pay() 호출하면 바로 실행될까, 대기할까?
 * 3. cancelOrder()에서 대기한다면 어느 큐에 들어갈까?
 * 4. paymentLock의 큐에도 영향이 있을까?
 * 5. orderLock.unlock()이 호출되면 누가 깨어날 수 있을까?
 *
 * Run:
 *   gradle runQuiz
 */
public class ReentrantLockQuiz {

    public static void main(String[] args) throws Exception {
        OrderService service = new OrderService();
        CountDownLatch updateOrderEntered = new CountDownLatch(1);
        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> service.updateOrder(start, updateOrderEntered));

            updateOrderEntered.await();

            executor.submit(() -> service.cancelOrder(start));
            executor.submit(() -> service.pay(start));
        }
    }

    static class OrderService {

        private final ReentrantLock orderLock = new ReentrantLock();
        private final ReentrantLock paymentLock = new ReentrantLock();

        void updateOrder(Instant start, CountDownLatch entered) {
            log(start, "Thread-A tries orderLock for updateOrder()");
            orderLock.lock();
            try {
                log(start, "Thread-A acquired orderLock");
                entered.countDown();
                sleep(1000);
                log(start, "Thread-A updateOrder done");
            } finally {
                orderLock.unlock();
                log(start, "Thread-A released orderLock");
            }
        }

        void cancelOrder(Instant start) {
            log(start, "Thread-B tries orderLock for cancelOrder()");
            orderLock.lock();
            try {
                log(start, "Thread-B acquired orderLock");
                sleep(1000);
                log(start, "Thread-B cancelOrder done");
            } finally {
                orderLock.unlock();
                log(start, "Thread-B released orderLock");
            }
        }

        void pay(Instant start) {
            log(start, "Thread-C tries paymentLock for pay()");
            paymentLock.lock();
            try {
                log(start, "Thread-C acquired paymentLock");
                sleep(1000);
                log(start, "Thread-C pay done");
            } finally {
                paymentLock.unlock();
                log(start, "Thread-C released paymentLock");
            }
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

    private static void log(Instant start, String message) {
        long elapsedMillis = Duration.between(start, Instant.now()).toMillis();
        System.out.printf("%4dms %-70s %s%n", elapsedMillis, message, Thread.currentThread());
    }
}
