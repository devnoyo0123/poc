#!/bin/bash

# Avro POC - Local 실행 스크립트
# Docker Compose로 Kafka + Schema Registry 시작 후 애플리케이션 실행

set -e  # 에러 발생 시 중지

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "🚀 Avro POC - Local Environment Setup"
echo "=========================================="
echo ""

# 1. Docker Compose로 Kafka + Schema Registry 시작
echo "📦 Step 1: Starting Kafka and Schema Registry with Docker Compose..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be ready..."
sleep 10

# Kafka가 준비될 때까지 대기
echo "🔍 Checking Kafka connectivity..."
until docker-compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 &> /dev/null; do
    echo "⏳ Waiting for Kafka to be ready..."
    sleep 2
done

echo "✅ Kafka is ready!"

# Schema Registry가 준비될 때까지 대기
echo "🔍 Checking Schema Registry connectivity..."
until curl -s http://localhost:8081/subjects > /dev/null; do
    echo "⏳ Waiting for Schema Registry to be ready..."
    sleep 2
done

echo "✅ Schema Registry is ready!"
echo ""

# 2. 애플리케이션 실행
echo "🚀 Step 2: Starting Spring Boot Application..."
echo ""

export APP_ENV=local
./gradlew bootRun

# 애플리케이션이 종료되면 Docker Compose 정리 여부 묻기
echo ""
echo "=========================================="
echo "🛑 Application stopped"
echo "=========================================="
echo ""
read -p "❓ Do you want to stop Docker Compose? (y/N) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🛑 Stopping Docker Compose..."
    docker-compose down
    echo "✅ Done!"
else
    echo "ℹ️  Docker Compose is still running. Stop manually with:"
    echo "   cd $SCRIPT_DIR && docker-compose down"
fi
