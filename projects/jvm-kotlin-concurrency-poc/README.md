# jvm-kotlin-concurrency-poc

Kotlin으로 JVM 동시성 도구를 producer-consumer 문제 하나로 비교하며 익히는 POC.
같은 작업(N producer × M consumer, 10만 아이템)을 6가지 방식으로 풀고 **정확성 + 처리량**을 비교한다.

## 핵심 질문: "Kotlin = Java syntax sugar?"

반은 맞고 반은 틀림.

- **맞다**: Kotlin/JVM은 같은 바이트코드로 컴파일된다. `synchronized`, `ReentrantLock`,
  `BlockingQueue`는 전부 `java.util.concurrent` 클래스를 그대로 쓴다. JMM(메모리 모델) 동일.
- **틀리다**: 코루틴은 syntax sugar가 **아니다**. 컴파일러가 `suspend` 함수를
  상태머신(CPS 변환)으로 바꾼다 — Java에 없는 진짜 기능. 블로킹(스레드) vs 서스펜드(코루틴)는
  실행 모델 자체가 다르다.

## 6가지 방식

| # | 방식 | 모델 | 배우는 것 |
|---|------|------|----------|
| 1 | `synchronized` + wait/notify | 스레드 블로킹 | 모니터 락, 조건대기 수동, while 재검사 함정 |
| 2 | `ReentrantLock` + `Condition` | 스레드 블로킹 | 명시적 락, notFull/notEmpty 분리, tryLock/fair/재진입 |
| 3 | `ArrayBlockingQueue` | 스레드 블로킹 | j.u.c 정석. 내부가 곧 2번 패턴 |
| 4 | `ConcurrentBag` (HikariCP식 미니 재구현) | lock-free 자원풀 | ThreadLocal 캐시 + CAS steal + handoff |
| 5 | `Channel` | 코루틴 서스펜드 | "코루틴용 BlockingQueue", 구조적 동시성 |
| 6 | `Mutex` + `Flow` | 코루틴 서스펜드 | 코루틴 락(비재진입), 콜드 스트림 backpressure |

> ⚠️ `ConcurrentBag`는 .NET의 동명 클래스와 다르고, JVM 표준에도 없다. HikariCP 커넥션 풀
> 내부 자료구조다. 단방향 파이프가 아니라 **재사용 자원 풀**(borrow→requite)이라 4번만
> 형태가 다르고 별도 벤치를 가진다.

## 실행

```bash
# 전체 비교 (1,2,3,5,6 한 표로)
./gradlew run

# 개별 예제
./gradlew runExample -PmainClass=com.example.concurrency.c01synchronized.SynchronizedBufferKt
./gradlew runExample -PmainClass=com.example.concurrency.c02reentrantlock.ReentrantLockBufferKt
./gradlew runExample -PmainClass=com.example.concurrency.c03blockingqueue.BlockingQueueBufferKt
./gradlew runExample -PmainClass=com.example.concurrency.c05channel.ChannelPipelineKt
./gradlew runExample -PmainClass=com.example.concurrency.c06mutexflow.MutexAndFlowKt

# 4번 ConcurrentBag vs BlockingQueue 풀 벤치
./gradlew runExample -PmainClass=com.example.concurrency.c04concurrentbag.ConcurrentBagBenchKt
```

## 정확성 검증 방식

카운트만 비교하면 "하나 유실 + 하나 중복" 버그를 놓친다. 그래서 각 아이템에 고유 정수값을
주고 producer/consumer 양쪽에서 **합(checksum)**을 누적한다. 두 합이 같아야 OK.
값은 1..N을 한 번씩 쓰므로 기대 합 = N(N+1)/2.

## 구조

```
common/Harness.kt        시나리오 정의, Checksums 검증, Result 출력
common/ThreadRunner.kt   1~4번 공유: N×M 스레드 + 독약(poison pill) 종료. buffer만 교체
c01..c03                 ItemBuffer 구현체 3종 (락 종류만 다름)
c04concurrentbag         ConcurrentBag 미니 재구현 + borrow/requite 벤치
c05channel               Channel 코루틴 파이프라인
c06mutexflow             Mutex 카운터 데모 + Flow 파이프라인
RunAll.kt                전체 비교 진입점
```

## 관전 포인트

- **1 vs 2**: `notifyAll`(헛깨움 많음) → `Condition` 2개로 정확히 깨우기.
- **2 vs 3**: 직접 만든 게 `ArrayBlockingQueue` 내부와 같음을 확인.
- **3 vs 5**: 같은 backpressure(capacity 1024)를 스레드 블로킹 vs 코루틴 서스펜드로.
- **4**: ThreadLocal 1층 적중으로 락 경쟁 회피. 단 **만능 아님** — 작은 풀/낮은 경쟁에선
  단일 락 `ArrayBlockingQueue`가 더 빠르다. ConcurrentBag은 스레드 수↑ + 자기 자원 재사용
  (실제 커넥션 풀) 패턴에서 빛난다. 벤치가 이 트레이드오프를 직접 보여줌.
- **6 Mutex**: 코루틴 안에선 `ReentrantLock` 말고 `Mutex`. 단 재진입 안 됨(교착 주의).
