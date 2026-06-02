#!/bin/bash

# 테스트용 로그 파일 생성 스크립트 (macOS)
BASE_DIR="/tmp/spring-batch-logs"

echo "📁 Creating test directory: $BASE_DIR"
rm -rf "$BASE_DIR"
mkdir -p "$BASE_DIR"

echo "📝 Creating test log files..."

# 오래된 파일 (10일 전)
touch -t $(date -v-10d +%Y%m%d%H%M) "$BASE_DIR/old-app-1.log"
touch -t $(date -v-10d +%Y%m%d%H%M) "$BASE_DIR/old-app-2.log"
touch -t $(date -v-15d +%Y%m%d%H%M) "$BASE_DIR/ancient-system.log"

# 최근 파일 (2일 전)
touch -t $(date -v-2d +%Y%m%d%H%M) "$BASE_DIR/recent-app-1.log"
touch -t $(date -v-2d +%Y%m%d%H%M) "$BASE_DIR/recent-app-2.log"

# 오늘 파일
touch "$BASE_DIR/today-debug.log"

echo ""
echo "✅ Test files created in: $BASE_DIR"
echo ""
echo "📋 File list:"
ls -lh "$BASE_DIR"
echo ""
echo "📅 File dates:"
ls -l "$BASE_DIR"
