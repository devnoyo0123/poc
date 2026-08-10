# kafka-consumer-group-poc

"인스턴스 1개에서 컨슈머를 여러 개 돌리는 법"과 **Consumer Group / Rebalance / 매뉴얼 커밋**을
눈으로 확인하기 위한 POC.

- `mvc/` — Spring Kafka (`@KafkaListener` + `concurrency`) 서블릿 스택
- `reactor/` — reactor-kafka (`KafkaReceiver` + `groupBy(partition)`) 리액티브 스택

두 앱 모두 6 파티션 토픽(`demo-topic`)을 소비하며, 리밸런스 시
`onPartitionsRevoked` / `onPartitionsAssigned` 로그를 찍는다.

---

## 핵심 개념 요약

### 1. 인스턴스 1개에 컨슈머 여러 개
- **MVC**: `ConcurrentMessageListenerContainer`. `concurrency=N` 이면 스레드 N개 = `KafkaConsumer` N개 (같은 그룹).
- **Reactor**: `KafkaReceiver` 를 N개 생성하거나, 한 receiver 안에서 `groupBy(partition)` 으로 병렬 처리.
- 공통 제약: **동시성 ≤ 파티션 수**. 파티션보다 많은 컨슈머/스레드는 논다(idle).

### 2. Consumer Group 은 누가 관리하나
- **브로커(Group Coordinator)**: 멤버십/heartbeat 관리, 리밸런스 트리거, 오프셋 저장(`__consumer_offsets`).
- **클라이언트(Group Leader)**: 실제 파티션→컨슈머 **할당 계산**. 리더 = 그 세대에 JoinGroup 을 가장 먼저 보낸 컨슈머.

### 3. 리밸런스 시퀀스 (Eager)
트리거 → REVOKE(전체 반납, stop-the-world) → JoinGroup → 리더 지정/generation++ →
리더가 할당 계산 → SyncGroup → 각자 파티션 받고 ASSIGNED → 컨슈밍 재개.
> 본 POC 는 `CooperativeStickyAssignor` 를 써서 "이동이 필요한 파티션만" 회수(점진적 리밸런스)한다.

### 4. 오프셋 커밋 ↔ 리밸런스
- 리밸런스로 파티션이 넘어가면 새 컨슈머는 **마지막 커밋된 오프셋**부터 읽는다.
- 커밋 안 된 in-flight 메시지는 **재처리(중복)** → 멱등성 필수.
- 본 POC 는 둘 다 **매뉴얼 커밋**(auto-commit OFF, 처리 후 ack) → at-least-once.

---

## 실행

### 0) 인프라 기동 + 토픽 생성 (6 파티션)
```bash
cd kafka-consumer-group-poc
docker compose up -d
./create-topic.sh          # demo-topic, 6 partitions
```
Kafka UI: http://localhost:8090

### A) MVC POC — concurrency 와 리밸런스 관찰

```bash
cd mvc
./gradlew bootRun                                  # 인스턴스1 (port 8080, concurrency=3)
```
다른 터미널에서 메시지 발행:
```bash
curl -X POST "http://localhost:8080/produce?count=60"
```
로그에서 확인할 것:
- `[REBALANCE] << ASSIGNED ... partitions=[0,1,2,3,4,5]` — 인스턴스1이 6파티션 전부 잡음
- `consume thread=...-0-C-1 partition=...` — 스레드 3개가 파티션 나눠 처리

이제 **두 번째 인스턴스**를 띄워 리밸런스 발생:
```bash
cd mvc
./gradlew bootRun --args='--server.port=8081'      # 인스턴스2
```
양쪽 로그에서 확인:
- 인스턴스1: `>> REVOKE partitions=[3,4,5]` (일부만 반납 — Cooperative)
- 인스턴스2: `<< ASSIGNED partitions=[3,4,5]`
- 결과적으로 각 인스턴스가 3파티션씩 → 부하 분산

인스턴스2를 Ctrl+C 로 끄면 다시 인스턴스1이 6파티션을 회수하는 리밸런스가 보인다.

### B) Reactor POC — KafkaReceiver / groupBy 병렬

```bash
cd reactor
./gradlew bootRun                                  # port 8082, receiver 1개
curl -X POST "http://localhost:8082/produce?count=60"
```
- `consume receiver#0 thread=parallel-... partition=...` — groupBy 로 파티션별 병렬 처리
- receiver 여러 개로 그룹 내 분산을 보고 싶으면:
  ```bash
  ./gradlew bootRun --args='--app.receivers=3'
  ```
  → ASSIGNED 로그가 receiver#0/#1/#2 로 나뉘어 찍힌다.

> 주의: MVC(`demo-group`)와 Reactor(`demo-group-reactor`)는 **다른 그룹**이라 같은 메시지를
> 각각 따로 받는다. 같은 그룹으로 묶고 싶으면 group-id 를 맞추면 된다.

### 정리
```bash
docker compose down -v
```

---

## 관찰 포인트 치트시트

| 보고 싶은 것 | 어떻게 |
|---|---|
| 스레드 N개 = 컨슈머 N개 | MVC `consume thread=` 로그의 서로 다른 스레드 이름 |
| 동시성 ≤ 파티션 | concurrency=8, 파티션 6 으로 띄우면 2 스레드는 idle |
| 리밸런스 REVOKE/ASSIGNED | 인스턴스 추가/제거 시 `[REBALANCE]` 로그 |
| Cooperative(점진적) | 전체가 아니라 "일부 파티션만" 반납되는 것 |
| 매뉴얼 커밋 효과 | 인스턴스 죽였다 살려도 커밋 지점부터만 재소비 |
| group-id 의미 | 같은 group-id=분산 / 다른 group-id=각자 전체 수신 |
