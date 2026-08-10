# virtual-thread-pinning-poc

Java 21 virtual thread pinning demo.

Run:

```bash
gradle run
```

Run lock quiz:

```bash
gradle runQuiz
```

JVM args:

- `-Djdk.virtualThreadScheduler.parallelism=1`
- `-Djdk.tracePinnedThreads=full`

What to observe:

- `CountDownLatch` works with virtual threads.
- `synchronized` around blocking code prints a JVM pinned-thread trace with `reason:MONITOR`.
- `ReentrantLock` waiting is virtual-thread-friendly and does not print the same monitor pinning trace.
- `ReentrantLock` still serializes protected work. It is not a throughput fix by itself.
- Virtual threads help I/O wait scalability, not CPU work or downstream limits.

Representative pinned trace:

```text
VirtualThread[...] reason:MONITOR
...
Thread.sleep(...)
... <== monitors:1
```
