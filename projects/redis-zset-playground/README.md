# Redis Sorted Set Playground

> Redis ZSet 실무 패턴 4가지를 직접 실행하며 배우는 학습용 프로젝트
>
> Kotlin + JDK 21 + Spring Boot 3.3 + Spring Data Redis

## 학습 목표

- Sorted Set (ZSet) 명령어를 **직접 실행**하며 동작 이해
- 각 명령어의 **redis-cli 원시 명령 ↔ Spring Data Redis API** 매핑 학습
- 실무에서 ZSet이 어떤 시나리오에 쓰이는지 패턴으로 체득

## 시나리오 4가지

| # | 패턴 | score | member | 핵심 명령 |
|---|------|-------|--------|----------|
| 1 | 🏆 실시간 랭킹보드 | 게임 점수 | 유저 ID | `ZADD`, `ZREVRANGE`, `ZREVRANK`, `ZINCRBY` |
| 2 | 🎟️ 선착순 이벤트 | timestamp | 유저 ID | `ZADD`, `ZCARD`, `ZRANGE`, `ZRANK` |
| 3 | ⏰ 지연 큐 | 실행 시각 | 작업 ID | `ZADD`, `ZRANGEBYSCORE`, `ZREM` |
| 4 | 🔍 최근 검색어 | timestamp | 검색어 | `ZADD`, `ZRANGE`, `ZREMRANGEBYRANK` |

## 실행 방법

### 1. Redis 컨테이너 실행

```bash
docker compose up -d
```

### 2. 애플리케이션 실행 (인터랙티브 메뉴)

```bash
./gradlew bootRun
```

메뉴가 나오면 번호 선택:
```
┌─────────────────────────────────────┐
│  메뉴 선택                          │
├─────────────────────────────────────┤
│  1. 🏆 실시간 랭킹보드              │
│  2. 🎟️  선착순 이벤트               │
│  3. ⏰  지연 큐                     │
│  4. 🔍 최근 검색어                  │
│  5. 🚀 전체 실행                    │
│  0. 종료                            │
└─────────────────────────────────────┘
```

### 3. 동시에 redis-cli로 직접 확인

다른 터미널에서:
```bash
docker exec -it zset-playground-redis redis-cli

127.0.0.1:6379> KEYS *
127.0.0.1:6379> ZRANGE game:rank 0 -1 WITHSCORES
127.0.0.1:6379> ZCARD fcfs:event:coupon-1000
```

## 출력 포맷

각 예제는 단계별로 다음을 출력:

```
  redis> ZADD game:rank 1500 user:1                  ← redis-cli 원시 명령어
  kotlin> redis.opsForZSet().add("game:rank", ...)   ← Spring API 호출
  → user:1 점수: 1500 등록                            ← 실행 결과
  // ※ ZADD = insert or update                       ← 핵심 개념 메모
```

## 정리

```bash
# Redis 컨테이너 종료 + 데이터 삭제
docker compose down -v
```

## 프로젝트 구조

```
redis-zset-playground/
├── settings.gradle.kts
├── build.gradle.kts
├── docker-compose.yml                  # Redis 7-alpine
└── src/main/
    ├── resources/application.yml
    └── kotlin/com/example/zsetplayground/
        ├── ZSetPlaygroundApplication.kt
        ├── config/RedisConfig.kt       # StringRedisSerializer 설정
        ├── runner/
        │   ├── Console.kt              # 출력 포맷 헬퍼
        │   └── MenuRunner.kt           # 인터랙티브 메뉴
        └── examples/
            ├── RankingExample.kt       # 🏆 랭킹보드
            ├── FcfsExample.kt          # 🎟️ 선착순
            ├── DelayQueueExample.kt    # ⏰ 지연큐
            └── RecentSearchExample.kt  # 🔍 최근검색
```

## 핵심 학습 포인트

### ZADD = Insert or Update (가장 중요)

같은 member를 다시 추가하면 **score만 갱신**됨. 새 멤버로 추가되지 않음.
→ 이 특성 때문에 선착순 중복 참여 방지, 최근 검색어 자동 갱신이 별도 로직 없이 구현됨.

### 인덱스 (음수 지원)

```
멤버: [A, B, C, D, E]
인덱스: 0  1  2  3  4
음수:  -5 -4 -3 -2 -1

ZRANGE key 0 -1     → 전체
ZRANGE key -3 -1    → 마지막 3개
ZRANGE key -1 -1    → 마지막 1개
```

### 시간복잡도

| 명령 | 복잡도 |
|------|--------|
| `ZADD` | O(log N) |
| `ZSCORE` | O(1) |
| `ZRANK` / `ZREVRANK` | O(log N) |
| `ZRANGE` | O(log N + M) |
| `ZCARD` | O(1) |

### 내부 구조

**Skip List + Hash Table**
- Skip List: 정렬 유지 (삽입/조회 O(log N))
- Hash Table: member → score 매핑 (O(1) 조회)
