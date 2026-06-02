#!/bin/bash

# Avro POC - Quick Start 스크립트
# Docker Compose 시작 → 애플리케이션 실행 → 간단한 테스트

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "🚀 Avro POC - Quick Start Guide"
echo "=========================================="
echo ""

echo "이 스크립트는 다음 순서로 실행합니다:"
echo "  1. Docker Compose로 Kafka + Schema Registry 시작"
echo "  2. 애플리케이션 빌드"
echo "  3. 테스트 실행 (Testcontainers)"
echo "  4. Docker Compose 정리 (선택)"
echo ""
read -p "❓ 계속하시겠습니까? (Y/n) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Nn]$ ]]; then
    echo "❌ 취소되었습니다."
    exit 0
fi

# 1. Docker Compose 시작
echo ""
echo "📦 Step 1: Starting Kafka and Schema Registry..."
docker-compose up -d

echo "⏳ Waiting for services..."
sleep 10

until curl -s http://localhost:8081/subjects > /dev/null; do
    echo "⏳ Waiting for Schema Registry..."
    sleep 2
done

echo "✅ Services are ready!"
echo ""

# 2. 빌드
echo "🔨 Step 2: Building project..."
./gradlew clean build -x test
echo "✅ Build completed!"
echo ""

# 3. 테스트
echo "🧪 Step 3: Running tests..."
./gradlew test
echo "✅ Tests completed!"
echo ""

# 4. 정리 여부 묻기
echo "=========================================="
echo "✅ Quick Start completed!"
echo "=========================================="
echo ""
echo "📊 Test Report: build/reports/tests/test/index.html"
echo ""
read -p "❓ Docker Compose를 정리하시겠습니까? (Y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    echo "🛑 Stopping Docker Compose..."
    docker-compose down
    echo "✅ Done!"
fi

echo ""
echo "💡 Next steps:"
echo "   1. 애플리케이션 실행: ./run-local.sh"
echo "   2. 테스트만 실행:    ./test.sh"
echo "   3. CLI 테스트:      ./test-with-avro-cli.sh"
