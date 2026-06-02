#!/bin/bash

# Avro POC - Quick Multi-Server Test
# 서버 2개 시작 → 설정 동기화 테스트 → 정리

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "🚀 Quick Multi-Server Test"
echo "=========================================="
echo ""

SERVER1_PORT=8088
SERVER2_PORT=8089

# 1. 서버 2개 시작
echo "Step 1: Starting 2 servers..."
./run-multi-server.sh &
MULTI_SERVER_PID=$!
echo "  Multi-server PID: $MULTI_SERVER_PID"
echo ""

# 서버 대기
echo "⏳ Waiting for servers to start..."
sleep 30

# 2. 설정 동기화 테스트
echo ""
echo "Step 2: Testing config sync..."
./test-config-sync.sh

# 3. 정리 여부 묻기
echo ""
read -p "❓ Stop servers? (Y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Nn]$ ]]; then
    echo "🛑 Stopping servers..."
    pkill -f "gradle.*bootRun"
    echo "✅ Done!"
fi

echo ""
echo "💡 To manually manage servers:"
echo "  ./run-multi-server.sh"
echo "  ./test-config-sync.sh"
