# Redis POC — 분산 채팅 아키텍처

Redis Pub/Sub + WebSocket 기반 채팅 시스템에서 **커넥션 서버와 비즈니스 서버를 분리**하여
단일 실패 지점(SPOF, Single Point of Failure)을 해소하는 POC.

## 아키텍처

```
                    ┌──────────────┐
                    │   nginx :80  │  (단순 라운드 로빈, sticky X)
                    └──────┬───────┘
              ┌────────────┼────────────┐
              │            │            │
       /ws/*  │      /api/*│            │ (WS Upgrade + REST)
              ▼            ▼            
   ┌──────────────────┐   ┌──────────────────┐
   │  ws-gateway-1    │   │   api-server     │
   │  :8081 (stateful)│   │   :8080 (stateless)
   │  WS 세션 로컬 관리 │   │  REST API + 발행  │
   └────────┬─────────┘   └─────────┬────────┘
            │                       │
   ┌──────────────────┐             │
   │  ws-gateway-2    │             │
   │  :8081 (stateful)│             │
   │  WS 세션 로컬 관리 │             │
   └────────┬─────────┘             │
            │                       │
            └───────────┬───────────┘
                        ▼
                 ┌─────────────┐
                 │ Redis :6379 │
                 │  Pub/Sub    │
                 │  (브로커)    │
                 └─────────────┘
```

### 메시지 흐름

```
[Client A] ──WS──▶ ws-gateway-1
                      │
                      │ 1. ChatInput 파싱
                      │ 2. ChatBroadcast 생성 (originServer=ws-gateway-1)
                      │ 3. redisTemplate.convertAndSend("chat:room1", payload)
                      ▼
                  Redis Pub/Sub
                  │   │   │
                  │   │   └──────────────────────┐
                  │   │                          │
                  ▼   ▼                          ▼
        ws-gateway-1  ws-gateway-2           (api-server)
         (발행자)       (다른 인스턴스)         (구독 안 함)
              │              │
              │ 4. 로컬 세션에 브로드캐스트     │ 4. 로컬 세션에 브로드캐스트
              ▼                               ▼
        [Client A]                       [Client B]
        (echo)                           (다른 서버 연결 클라)
```

## SPOF 해소 포인트

### 1. 커넥션/비즈니스 서버 분리 (Phase A)
- **이전**: WS 연결, REST API, 캐시 로직이 한 앱에 섞여 있어 어느 하나 장애 나면 전부 장애
- **이후**: 
  - WS 서버가 죽어도 REST API는 정상 → WS 클라이언트만 영향
  - API 서버가 죽어도 이미 맺어진 WS 연결은 유지 → 채팅 지속 가능
  - WS 서버만 수평 확장 (connection 수에 비례)

### 2. 다중 WS 인스턴스 + 로드밸런서 (Phase B)
- nginx가 ws-gateway-1, ws-gateway-2로 라운드 로빈
- 한 인스턴스 장애 시 클라이언트 재연결 → nginx가 살아있는 인스턴스로 라우팅
- Redis Pub/Sub이 모든 인스턴스에 메시지를 브로드캐스트하므로 어느 서버에 연결되든 같은 방 메시지 수신

### 3. (아직 미구현) Redis HA, 세션 복구 (Phase C)
- Redis Sentinel/Cluster
- WS 재연결 시 밀린 메시지 복구 (Pub/Sub → Redis Stream)

## sticky session을 쓰지 않는 이유

일반적으로 WS 로드밸런싱에서는 같은 클라이언트를 항상 같은 서버로 보내는 sticky session(ip_hash 등)을 쓴다. **하지만 본 POC에서는 불필요**:

| sticky session이 필요한 일반적 사례 | 본 POC |
|---|---|
| 서버 메모리에 인증 세션 보관 | 세션 없음 (POC) |
| 서버 메모리에 장바구니/상태 보관 | 상태 없음 (WS 세션만) |
| REST API와 WS가 같은 서버에서 상태 공유 | REST는 api-server, WS는 ws-gateway로 분리 |
| SSE/SSE 기반 알림 | 사용 안 함 |

**핵심 이유**: Redis Pub/Sub이 fan-out 브로커 역할을 하므로, 어느 ws-gateway로 연결되든 모든 인스턴스가 같은 메시지를 받아 각자 로컬 세션에 전달한다. sticky session의 이점이 사라짐.

## 모듈 구조

```
redis-poc/
├── settings.gradle.kts              # multi-module 설정
├── build.gradle.kts                 # 루트 빌드 (공통 플러그인)
├── docker-compose.yml               # Redis + ws-gateway x2 + api-server + nginx
├── docker/
│   └── nginx/nginx.conf             # WS upgrade + 라운드 로빈
├── shared/                          # 공통 라이브러리 (jar만 생성, bootJar X)
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/example/redis/shared/
│       ├── dto/ChatMessage.kt       # ChatInput, ChatBroadcast
│       ├── constant/ChatChannels.kt # 채널 네이밍 규칙
│       ├── config/RedisConfig.kt    # RedisTemplate, MessageListenerContainer
│       └── identity/ServerIdentity.kt # 인스턴스 식별자
├── ws-gateway/                      # WS 커넥션 서버 (stateful)
│   ├── build.gradle.kts
│   ├── Dockerfile
│   └── src/main/kotlin/com/example/redis/wsgateway/
│       ├── WsGatewayApplication.kt
│       ├── config/WebSocketConfig.kt
│       └── handler/ChatWebSocketHandler.kt
└── api-server/                      # 비즈니스 서버 (stateless)
    ├── build.gradle.kts
    ├── Dockerfile
    └── src/main/kotlin/com/example/redis/apiserver/
        ├── ApiServerApplication.kt
        ├── controller/
        │   ├── RedisController.kt   # String/Hash/SortedSet/Lock/Pipeline/PubSub API
        │   └── ChatController.kt    # REST로 채팅 메시지 발행
        └── service/                  # (기존 redis-poc 로직 이전)
```

## 실행

### 전체 스택 실행 (docker compose)

```bash
# 빌드 + 실행
docker compose up -d --build

# 상태 확인
docker compose ps

# 로그
docker compose logs -f ws-gateway-1 ws-gateway-2 api-server

# 헬스체크
curl http://localhost/nginx-health

# 종료
docker compose down -v
```

### 테스트 스크립트

```bash
./test-all.sh
# docker compose up + HTTP API 테스트 + Pub/Sub 상태 확인 자동화
```

### 개별 모듈 로컬 실행 (docker 없이)

```bash
# Redis만 docker로
docker compose up -d redis

# ws-gateway (8081)
./gradlew :ws-gateway:bootRun

# api-server (8080) - 다른 터미널
./gradlew :api-server:bootRun
```

## API 엔드포인트 (nginx 통해)

### 채팅

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/chat/send` | REST로 채팅 메시지 발행 (body: `{room, sender, content}`) |
| GET | `/api/chat/channels/{room}` | room의 Redis 채널명 조회 |

### WebSocket

| Endpoint | Path |
|---|---|
| WS | `ws://localhost/ws/chat` (nginx 통해 라운드 로빈) |
| WS | `ws://localhost:8081/ws/chat` (ws-gateway-1 직접) |
| WS | `ws://localhost:8082/ws/chat` (ws-gateway-2 직접) |

WS 메시지 포맷 (서버로 전송):
```json
{ "room": "room1", "sender": "alice", "content": "hello" }
```

### 일반 Redis API (api-server)

| Category | Endpoints |
|---|---|
| String | `/api/string/product/{id}` (GET, POST, DELETE), `/ttl`, `/decrease-stock` |
| Hash | `/api/hash/product/{id}` (GET, POST), `/decrease-stock`, `/update-name`, `/counter/{apiName}` |
| Sorted Set | `/api/leaderboard/add`, `/top`, `/rank/{id}`, `/increment`, `/range` |
| Lock | `/api/lock/acquire`, `/release`, `/product/{id}/decrease-stock` |
| Pipeline | `/api/pipeline/individual`, `/batch`, `/get-multiple` |
| Pub/Sub | `/api/pubsub/subscribe`, `/publish`, `/history` |

## WebSocket 클라이언트 테스트

`websocat` 설치 후 두 터미널에서:

```bash
# Terminal 1 - ws-gateway-1 직접 연결
websocat ws://localhost:8081/ws/chat
{"room":"room1","sender":"alice","content":"hi from alice"}

# Terminal 2 - ws-gateway-2 직접 연결 (다른 인스턴스!)
websocat ws://localhost:8082/ws/chat
{"room":"room1","sender":"bob","content":"hi from bob"}
```

**검증 포인트**:
- alice가 보낸 메시지를 bob이 수신 (서로 다른 ws-gateway 인스턴스인데도)
- originServer 필드로 어느 인스턴스가 처리했는지 확인 가능

또는 nginx 통해 라운드 로빈 테스트:

```bash
# 여러 클라이언트가 nginx:80/ws/chat으로 연결 → 자동으로 ws-gateway-1/2에 분산
websocat ws://localhost/ws/chat
```

## 핵심 학습 포인트

1. **서버 분리의 기준**: stateful(WS 세션) vs stateless(비즈니스 로직)
2. **Redis Pub/Sub의 브로커 역할**: 다중 인스턴스 간 상태 동기화 없이 메시지 전달
3. **sticky session의 한계**: 인메모리 상태 의존도에 따라 선택, 분산 아키텍처에서는 불필요할 때가 많음
4. **SPOF 해소 우선순위**: 앱 서버 분리 → 다중 인스턴스 → Redis HA → 세션 복구
