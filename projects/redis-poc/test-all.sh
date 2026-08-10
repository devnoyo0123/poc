#!/usr/bin/env bash
# 통합 테스트 스크립트 (HTTP/REST 기반)
#
# WS 클라이언트 테스트는 별도 도구(websocat/wscat)가 필요하므로
# README의 WS 섹션을 참고하여 수동으로 실행.
set -euo pipefail

cd "$(dirname "$0")"

echo "=== [1/6] Docker Compose 빌드 + 실행 ==="
docker compose down -v 2>/dev/null || true
docker compose up -d --build

echo ""
echo "=== [2/6] 서비스 헬스체크 대기 (최대 30초) ==="
for i in {1..30}; do
    if curl -sf http://localhost/nginx-health >/dev/null 2>&1; then
        echo "nginx ready (after ${i}s)"
        break
    fi
    sleep 1
done

echo ""
echo "=== [3/6] nginx 헬스체크 ==="
curl -s http://localhost/nginx-health

echo ""
echo "=== [4/6] 채팅 메시지 발행 (REST → Redis Pub/Sub) ==="
echo "첫 번째 메시지를 보내면 ws-gateway가 구독하므로 receivers >= 1 이어야 함"
echo ""
echo "--- 1) 채팅 메시지 발행 ---"
curl -s -X POST http://localhost/api/chat/send \
    -H "Content-Type: application/json" \
    -d '{"room":"test-room","sender":"api-bot","content":"hello from REST"}' | jq .

echo ""
echo "--- 2) 일반 Redis API (String 캐시) ---"
curl -s -X POST http://localhost/api/string/product/1 \
    -H "Content-Type: application/json" \
    -d '{"name":"TestProduct","price":1000,"stock":10}' | jq .

curl -s http://localhost/api/string/product/1 | jq .

echo ""
echo "--- 3) Hash 캐시 ---"
curl -s -X POST "http://localhost/api/hash/product/2?name=HashItem&price=500&stock=7" | jq .
curl -s http://localhost/api/hash/product/2 | jq .

echo ""
echo "--- 4) Pub/Sub 일반 채널 ---"
curl -s -X POST "http://localhost/api/pubsub/subscribe?channel=alerts" | jq .
curl -s -X POST "http://localhost/api/pubsub/publish?channel=alerts&message=test-alert" | jq .

echo ""
echo "=== [5/6] Redis Pub/Sub 채널 상태 확인 ==="
docker compose exec -T redis redis-cli PUBSUB CHANNELS "chat:*"
echo ""
echo "--- chat:test-room 구독자 수 (WS 클라이언트가 없으면 0) ---"
docker compose exec -T redis redis-cli PUBSUB NUMSUB chat:test-room

echo ""
echo "=== [6/6] 각 ws-gateway 인스턴스 상태 (로그 tail) ==="
echo "--- ws-gateway-1 로그 ---"
docker compose logs --tail=20 ws-gateway-1
echo ""
echo "--- ws-gateway-2 로그 ---"
docker compose logs --tail=20 ws-gateway-2

echo ""
echo "============================================"
echo "HTTP 테스트 완료. WS 클라이언트 테스트는 README를 참조."
echo ""
echo "컨테이너 상태:"
docker compose ps
echo ""
echo "로그 모니터링: docker compose logs -f"
echo "종료: docker compose down -v"
