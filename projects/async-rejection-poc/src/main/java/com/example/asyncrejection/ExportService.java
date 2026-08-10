package com.example.asyncrejection;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final AtomicReference<CountDownLatch> started = new AtomicReference<>(new CountDownLatch(1));
    private final AtomicReference<String> lastWorkerThread = new AtomicReference<>();

    @Async("excelExportTaskExecutor")
    public void exportAsync(UUID jobId, long sleepMillis) {
        String workerThread = Thread.currentThread().getName();
        lastWorkerThread.set(workerThread);
        started.get().countDown();

        log.info("async job started. jobId={}, workerThread={}", jobId, workerThread);

        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("async job interrupted", exception);
        }

        log.info("async job finished. jobId={}, workerThread={}", jobId, workerThread);
    }

    void resetProbe() {
        started.set(new CountDownLatch(1));
        lastWorkerThread.set(null);
    }

    boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
        return started.get().await(timeout, unit);
    }

    String lastWorkerThread() {
        return lastWorkerThread.get();
    }
}
