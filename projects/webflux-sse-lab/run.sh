#!/bin/bash

# WebFlux + SSE 실습 랩 실행 스크립트

echo "🚀 WebFlux + SSE Lab 시작 중..."
echo ""

# Gradle Wrapper가 없으면 다운로드
if [ ! -f "gradlew" ]; then
    echo "📦 Gradle Wrapper 다운로드 중..."
    gradle wrapper --gradle-version 8.5
fi

# 의존성 설치
echo "📦 의존성 설치 중..."
./gradlew build -x test

# 애플리케이션 실행
echo ""
echo "✅ 애플리케이션 시작 중..."
echo "📍 SSE: http://localhost:8080/api/sse/notifications"
echo "📍 REST API: http://localhost:8080/api/users"
echo ""
echo "종료하려면 Ctrl+C"
echo ""

./gradlew bootRun
