package com.poc.asynccontext.controller;

import com.poc.asynccontext.dto.AggregatedResponse;
import com.poc.asynccontext.dto.ResponseA;
import com.poc.asynccontext.dto.ResponseB;
import com.poc.asynccontext.dto.ResponseC;
import com.poc.asynccontext.service.ExternalServiceA;
import com.poc.asynccontext.service.ExternalServiceB;
import com.poc.asynccontext.service.ExternalServiceC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 피닝(Pinning) 시연 컨트롤러.
 * 전제: spring.threads.virtual.enabled=true (요청이 가상 스레드에서 처리됨)
 * 실행: -Djdk.tracePinnedThreads=full 플래그 켜고 부하 주면 피닝 발생 시 스택 출력(Java 21).
 *
 *  /pinning/bad  : synchronized 안에서 블록 → 캐리어(OS 스레드) 고정 = 피닝 발생
 *  /pinning/good : ReentrantLock 으로 교체 → 언마운트 정상 = 피닝 없음
 */
@RestController
@RequestMapping("/pinning")
public class PinningController {

    private final ExternalServiceA serviceA;
    private final ExternalServiceB serviceB;
    private final ExternalServiceC serviceC;

    // synchronized 데모용 모니터 객체
    private final Object monitor = new Object();
    // 해결책 데모용 락
    private final ReentrantLock lock = new ReentrantLock();

    public PinningController(ExternalServiceA serviceA,
                            ExternalServiceB serviceB,
                            ExternalServiceC serviceC) {
        this.serviceA = serviceA;
        this.serviceB = serviceB;
        this.serviceC = serviceC;
    }

    /**
     * 나쁜 예: synchronized 블록 안에서 블로킹 호출.
     * 가상 스레드가 join()에서 블록되는데 synchronized 모니터를 쥐고 있어
     * 캐리어 OS 스레드에서 언마운트 못 함 → 피닝 → OS 스레드도 같이 1초 묶임.
     */
    @GetMapping("/bad")
    public AggregatedResponse bad() {
        long start = System.currentTimeMillis();
        synchronized (monitor) {                 // ★ 모니터 진입
            return aggregate(start, "PINNED");   // 이 안의 join()이 피닝 유발
        }
    }

    /**
     * 좋은 예: 동일 로직을 ReentrantLock 으로.
     * ReentrantLock 점유 중 블록돼도 가상 스레드는 캐리어에서 정상 언마운트 → 피닝 없음.
     */
    @GetMapping("/good")
    public AggregatedResponse good() {
        long start = System.currentTimeMillis();
        lock.lock();                             // ★ ReentrantLock 진입
        try {
            return aggregate(start, "NOT_PINNED");
        } finally {
            lock.unlock();
        }
    }

    private AggregatedResponse aggregate(long start, String mode) {
        CompletableFuture<ResponseA> fa = serviceA.callAsync();
        CompletableFuture<ResponseB> fb = serviceB.callAsync();
        CompletableFuture<ResponseC> fc = serviceC.callAsync();

        CompletableFuture.allOf(fa, fb, fc).join();  // 블록 지점

        long elapsed = System.currentTimeMillis() - start;
        return new AggregatedResponse(fa.join(), fb.join(), fc.join(), elapsed, mode);
    }
}