# Large Scale Chat Service POC

이 디렉터리는 대규모 라이브 채팅 시스템을 공부하고 설계 결정을 쌓아가는 POC 출발점이다.

지금은 구현보다 설계 정리에 집중한다. 범위가 다시 흔들리지 않도록 아래 순서로 본다.

## 먼저 볼 문서

- [설계 노트](./docs/poc-design-note.md)
- [기존 아키텍처 정리](./docs/large-scale-chat-architecture.md)
- [대화형 논의 노트](./docs/chat-architecture-discussion-notes.md)

## 지금 POC의 목표

- 라이브 채팅 시스템의 정상 흐름을 한 번에 설명할 수 있어야 한다.
- 장애가 어디서 생기고 어디까지 전파되는지 분리해서 볼 수 있어야 한다.
- 구현은 한 번에 크게 하지 않고, 패턴별로 잘라서 검증한다.

## 지금 하지 않는 것

- 100만 동접 전체 구현
- HOT channel 최적화
- 샤딩/멀티 리전/글로벌 배포
- 인증/권한의 세부 구현

## 추천 진행 순서

1. Ordering
2. Delivery semantics
3. Idempotency
4. Retry / timeout
5. Circuit breaker / bulkhead
6. Backpressure
7. Recovery / replay
8. HOT channel / sharding
