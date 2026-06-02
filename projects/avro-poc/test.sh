#!/bin/bash

# Avro POC - 테스트 스크립트
# Testcontainers로 자동으로 Kafka + Schema Registry 띄워서 테스트

set -e  # 에러 발생 시 중지

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "🧪 Avro POC - Test Execution"
echo "=========================================="
echo ""

# 테스트 옵션 선택
echo "선택 옵션:"
echo "  1. 전체 테스트 (단위 + 통합)"
echo "  2. 단위 테스트만 (Mock 기반)"
echo "  3. 통합 테스트만 (Testcontainers 기반)"
echo ""
read -p "❓ 선택하세요 (1-3): " -n 1 -r
echo ""
echo ""

case $REPLY in
    1)
        echo "🧪 Running all tests..."
        ./gradlew test
        ;;
    2)
        echo "🧪 Running unit tests only..."
        ./gradlew test --tests "*ProducerTest"
        ;;
    3)
        echo "🧪 Running integration tests only..."
        ./gradlew test --tests "*IntegrationTest"
        ;;
    *)
        echo "❌ Invalid choice. Running all tests..."
        ./gradlew test
        ;;
esac

echo ""
echo "=========================================="
echo "✅ Test execution completed!"
echo "=========================================="
echo ""
echo "📊 Test Report:"
echo "   HTML: build/reports/tests/test/index.html"
echo "   XML:  build/test-results/test/"
echo ""

# Test results 확인
if [ -d "build/test-results/test" ]; then
    echo "📈 Test Summary:"
    grep -h "tests=" build/test-results/test/TEST-*.xml 2>/dev/null | \
        awk -F'=' '{total+=$2; passed+=$3; failed+=$4; errors+=$5} END {
            printf "   Total: %d, Passed: %d, Failed: %d, Errors: %d\n", total, passed, failed, errors
        }' || echo "   (No test results found)"
fi
