#!/bin/bash

# 멀티 서버 실행 스크립트
# 서버 2개를 동시에 실행

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "🚀 Multi-Server Setup"
echo "=========================================="
echo ""

# 포트 설정
SERVER1_PORT=8088
SERVER2_PORT=8089

echo "📋 Ports: Server1=$SERVER1_PORT, Server2=$SERVER2_PORT"
echo ""

# 서버 1 시작
echo "🟢 Starting Server 1..."
SERVER_PORT=$SERVER1_PORT ./gradlew bootRun > server1.log 2>&1 &
SERVER1_PID=$!
echo "  PID: $SERVER1_PID, Log: server1.log"
sleep 15

# 서버 2 시작
echo "🟢 Starting Server 2..."
SERVER_PORT=$SERVER2_PORT ./gradlew bootRun > server2.log 2>&1 &
SERVER2_PID=$!
echo "  PID: $SERVER2_PID, Log: server2.log"
sleep 15

echo ""
echo "=========================================="
echo "✅ Both servers running!"
echo "=========================================="
echo ""
echo "📊 Server 1: http://localhost:$SERVER1_PORT (PID $SERVER1_PID)"
echo "📊 Server 2: http://localhost:$SERVER2_PORT (PID $SERVER2_PID)"
echo ""
echo "🧪 Test: curl -X POST http://localhost:$SERVER1_PORT/api/config/feature-flag -H 'Content-Type: application/json' -d '{\"key\":\"new-ui\",\"value\":true}'"
echo ""
echo "📋 Logs: tail -f server1.log server2.log"
echo ""
echo "🛑 Stop: kill $SERVER1_PID $SERVER2_PID"
echo ""

# Ctrl+C로 종료
trap "echo ''; echo '🛑 Stopping...'; kill $SERVER1_PID $SERVER2_PID 2>/dev/null; exit 0" INT TERM

while true; do
    sleep 1

    if ! kill -0 $SERVER1_PID 2>/dev/null; then
        echo "⚠️  Server 1 stopped"
        kill $SERVER2_PID 2>/dev/null
        exit 1
    fi

    if ! kill -0 $SERVER2_PID 2>/dev/null; then
        echo "⚠️  Server 2 stopped"
        kill $SERVER1_PID 2>/dev/null
        exit 1
    fi
done
