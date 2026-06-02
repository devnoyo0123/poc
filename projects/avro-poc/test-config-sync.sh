#!/bin/bash

# Avro POC - 설정 동기화 테스트 스크립트

SERVER1_PORT=8088
SERVER2_PORT=8089

echo "=========================================="
echo "🧪 Config Sync Test"
echo "=========================================="
echo ""

echo "📋 Test Scenario:"
echo "  1. Check initial config on both servers"
echo "  2. Update config on Server 1"
echo "  3. Verify Server 2 auto-updated via Kafka"
echo ""

# 1. 초기 설정 확인
echo "Step 1: Checking initial config..."
echo ""
echo "🟢 Server 1:"
curl -s http://localhost:$SERVER1_PORT/api/config | jq '.'
echo ""
echo "🟢 Server 2:"
curl -s http://localhost:$SERVER2_PORT/api/config | jq '.'
echo ""

# 2. 설정 변경 (Server 1에서)
echo "=========================================="
echo "Step 2: Updating config on Server 1..."
echo "=========================================="
echo ""

curl -X POST http://localhost:$SERVER1_PORT/api/config/feature-flag \
  -H "Content-Type: application/json" \
  -d '{
    "key": "new-ui",
    "value": true
  }' | jq '.'

echo ""
echo "⏳ Waiting for sync..."
sleep 3
echo ""

# 3. 동기화 확인
echo "=========================================="
echo "Step 3: Verifying config sync..."
echo "=========================================="
echo ""

echo "🟢 Server 1 (should be updated):"
SERVER1_CONFIG=$(curl -s http://localhost:$SERVER1_PORT/api/config)
echo "$SERVER1_CONFIG" | jq '.featureFlags.new-ui'
echo ""

echo "🟢 Server 2 (should be auto-updated via Kafka):"
SERVER2_CONFIG=$(curl -s http://localhost:$SERVER2_PORT/api/config)
echo "$SERVER2_CONFIG" | jq '.featureFlags.new-ui'
echo ""

# 결과 비교
SERVER1_VALUE=$(echo "$SERVER1_CONFIG" | jq -r '.featureFlags.new-ui')
SERVER2_VALUE=$(echo "$SERVER2_CONFIG" | jq -r '.featureFlags.new-ui')

echo "=========================================="
echo "📊 Test Results"
echo "=========================================="
echo ""

if [ "$SERVER1_VALUE" = "true" ] && [ "$SERVER2_VALUE" = "true" ]; then
    echo "✅ SUCCESS! Both servers have the same config:"
    echo "  Server 1: new-ui = $SERVER1_VALUE"
    echo "  Server 2: new-ui = $SERVER2_VALUE"
    echo ""
    echo "🎉 Config sync via Kafka is working!"
else
    echo "❌ FAILED! Configs are different:"
    echo "  Server 1: new-ui = $SERVER1_VALUE"
    echo "  Server 2: new-ui = $SERVER2_VALUE"
    echo ""
    echo "💡 Check:"
    echo "  - Are both servers running?"
    echo "  - Is Kafka running?"
    echo "  - Check server logs for errors"
fi

echo ""
echo "💡 View in Kafka UI:"
echo "  http://localhost:8080 → Topics → config-events → Messages"
