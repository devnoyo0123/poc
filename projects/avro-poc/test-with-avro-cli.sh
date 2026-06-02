#!/bin/bash

# Avro POC - Schema Registry 테스트 스크립트
# Avro CLI로 Schema 등록/조회 테스트

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "🔍 Avro POC - Schema Registry CLI Test"
echo "=========================================="
echo ""

# 1. Schema Registry 상태 확인
echo "📡 Step 1: Checking Schema Registry status..."
curl -s http://localhost:8081/subjects | jq '.' || echo "❌ Schema Registry is not running!"
echo ""

# 2. Schema 등록
echo "📝 Step 2: Registering UserEvent schema..."
SCHEMA_ID=$(curl -s -X POST \
    -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    --data '{"schema": "{\"type\":\"record\",\"name\":\"UserEvent\",\"namespace\":\"com.example.avro\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"long\"},{\"name\":\"eventType\",\"type\":[\"string\",\"null\"],\"default\":null}] }"}' \
    http://localhost:8081/subjects/user-events-value/versions | jq -r '.id')

echo "✅ Schema registered with ID: $SCHEMA_ID"
echo ""

# 3. Schema 조회
echo "🔍 Step 3: Retrieving schema by ID..."
curl -s http://localhost:8081/schemas/ids/$SCHEMA_ID | jq '.'
echo ""

# 4. Schema 버전 조회
echo "📋 Step 4: Listing schema versions..."
curl -s http://localhost:8081/subjects/user-events-value/versions | jq '.'
echo ""

# 5. Schema 호환성 테스트
echo "🧪 Step 5: Testing schema compatibility..."
curl -s -X POST \
    -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    --data '{"schema": "{\"type\":\"record\",\"name\":\"UserEvent\",\"namespace\":\"com.example.avro\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"timestamp\",\"type\":\"long\"},{\"name\":\"eventType\",\"type\":[\"string\",\"null\"],\"default\":null},{\"name\":\"newField\",\"type\":[\"string\",\"null\"],\"default\":null}] }"}' \
    http://localhost:8081/compatibility/subjects/user-events-value/versions/latest | jq '.'
echo ""

echo "✅ All tests completed!"
echo ""
echo "💡 Tips:"
echo "   - View all subjects: curl http://localhost:8081/subjects"
echo "   - View specific schema: curl http://localhost:8081/subjects/user-events-value/versions/latest"
echo "   - Delete schema: curl -X DELETE http://localhost:8081/subjects/user-events-value"
