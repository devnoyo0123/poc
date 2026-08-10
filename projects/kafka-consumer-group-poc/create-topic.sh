#!/usr/bin/env bash
# demo-topic 을 6 파티션으로 명시 생성 (auto-create 에 의존하지 않고 확정적으로 만들기 위함)
set -e

TOPIC="${1:-demo-topic}"
PARTITIONS="${2:-6}"

docker compose exec kafka kafka-topics \
  --bootstrap-server kafka:9093 \
  --create --if-not-exists \
  --topic "$TOPIC" \
  --partitions "$PARTITIONS" \
  --replication-factor 1

echo "---"
docker compose exec kafka kafka-topics \
  --bootstrap-server kafka:9093 \
  --describe --topic "$TOPIC"
