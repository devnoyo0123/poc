#!/bin/bash

# Avro POC - 멀티 서버 실행 스크립트
# 서버 2개를 동시에 실행해서 설정 동기화 테스트

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "🚀 Avro POC - Multi-Server Setup"
echo "=========================================="
echo ""

# 포트 설정
SERVER1_PORT=8088
SERVER2_PORT=8089

echo "📋 Configuration:"
echo "  Server 1: http://localhost:$SERVER1_PORT"
echo "  Server 2: http://localhost:$SERVER2_PORT"
echo ""

# 서버 1 시작 (background)
echo "🟢 Starting Server 1 on port $SERVER1_PORT..."
SERVER_PORT=$SERVER1_PORT ./gradlew bootRun > server1.log 2>&1 &
SERVER1_PID=$!
echo "  PID: $SERVER1_PID"
echo "  Log: server1.log"

# 서버 대기
echo ""
echo "⏳ Waiting for Server 1 to start..."
sleep 15

# 서버 2 시작 (background)
echo ""
echo "🟢 Starting Server 2 on port $SERVER2_PORT..."
SERVER_PORT=$SERVER2_PORT ./gradlew bootRun > server2.log 2>&1 &
SERVER2_PID=$!
echo "  PID: $SERVER2_PID"
echo "  Log: server2.log"

# 서버 대기
echo ""
echo "⏳ Waiting for Server 2 to start..."
sleep 15

echo ""
echo "=========================================="
echo "✅ Both servers are running!"
echo "=========================================="
echo ""
echo "📊 Server Status:"
echo "  Server 1: PID $SERVER1_PID (port $SERVER1_PORT)"
echo "  Server 2: PID $SERVER2_PID (port $SERVER2_PORT)"
echo ""
echo "🧪 Test Commands:"
echo ""
echo "1. Check Server 1 config:"
echo "   curl http://localhost:$SERVER1_PORT/api/config"
echo ""
echo "2. Check Server 2 config:"
echo "   curl http://localhost:$SERVER2_PORT/api/config"
echo ""
echo "3. Update config on Server 1:"
echo '   curl -X POST http://localhost:'$SERVER1_PORT'/api/config/feature-flag \'
echo "     -H 'Content-Type: application/json' \"
echo "     -d '{\"key\":\"new-ui\",\"value\":true}'"
echo ""
echo "4. Verify both servers have the same config:"
echo "   curl http://localhost:$SERVER1_PORT/api/config"
echo "   curl http://localhost:$SERVER2_PORT/api/config"
echo ""
echo "🛑 To stop servers:"
echo "   kill $SERVER1_PID $SERVER2_PID"
echo ""
echo "📋 Logs:"
echo "   tail -f server1.log"
echo "   tail -f server2.log"
echo ""
echo "Press Ctrl+C to stop both servers..."

# 대기 (사용자가 Ctrl+C 누를 때까지)
trap "echo ''; echo '🛑 Stopping servers...'; kill $SERVER1_PID $SERVER2_PID 2>/dev/null; exit 0" INT TERM

while true; do
    sleep 1

    # 프로세스 체크
    if ! kill -0 $SERVER1_PID 2>/dev/null; then
        echo "⚠️  Server 1 stopped unexpectedly"
        kill $SERVER2_PID 2>/dev/null
        exit 1
    fi

    if ! kill -0 $SERVER2_PID 2>/dev/null; then
        echo "⚠️  Server 2 stopped unexpectedly"
        kill $SERVER1_PID 2>/dev/null
        exit 1
    fi
done
