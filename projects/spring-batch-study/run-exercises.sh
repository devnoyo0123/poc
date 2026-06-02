#!/bin/bash

# Spring Batch Section 2 실습 실행 스크립트

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "🚀 Spring Batch Section 2 실습"
echo "================================"
echo ""

# Gradle wrapper가 없으면 생성
if [ ! -f "gradlew" ]; then
    echo "📦 Initializing Gradle wrapper..."
    gradle wrapper --gradle-version 8.5
fi

echo "실습을 선택하세요:"
echo ""
echo "1. Tasklet 기반 파일 삭제"
echo "2. JobParameters 데모"
echo "3. Chunk 기반 데이터 처리"
echo ""
read -p "선택 (1-3): " choice

case $choice in
    1)
        echo ""
        echo "📁 [실습 1] Tasklet 기반 파일 삭제"
        echo "=================================="
        echo ""
        
        # 테스트 파일 생성
        if [ -f "src/main/resources/scripts/setup-test-files.sh" ]; then
            bash src/main/resources/scripts/setup-test-files.sh
        fi
        
        echo ""
        echo "🔥 배치 실행 중..."
        ./gradlew bootRun --args='--spring.batch.job.name=deleteOldFilesJob basePath=/tmp/spring-batch-logs daysOld=7'
        ;;
        
    2)
        echo ""
        echo "📋 [실습 2] JobParameters 데모"
        echo "=============================="
        echo ""
        echo "파라미터 타입을 선택하세요:"
        echo "1. 기본 (name, age)"
        echo "2. 날짜 (targetDate)"
        echo "3. Enum (difficulty)"
        echo "4. 모두"
        echo ""
        read -p "선택 (1-4): " paramChoice
        
        case $paramChoice in
            1)
                ./gradlew bootRun --args='--spring.batch.job.name=parameterDemoJob name=John age=30'
                ;;
            2)
                ./gradlew bootRun --args='--spring.batch.job.name=parameterDemoJob name=Jane targetDate=2026-12-25,java.time.LocalDate'
                ;;
            3)
                ./gradlew bootRun --args='--spring.batch.job.name=parameterDemoJob name=Alice difficulty=HARD,com.example.batch.model.Difficulty'
                ;;
            4)
                ./gradlew bootRun --args='--spring.batch.job.name=parameterDemoJob name=Bob age=25 targetDate=2026-04-13,java.time.LocalDate difficulty=INSANE,com.example.batch.model.Difficulty'
                ;;
            *)
                echo "❌ 잘못된 선택"
                exit 1
                ;;
        esac
        ;;
        
    3)
        echo ""
        echo "📊 [실습 3] Chunk 기반 데이터 처리"
        echo "==================================="
        echo ""
        
        if [ ! -f "data.csv" ]; then
            echo "❌ data.csv 파일을 찾을 수 없습니다."
            exit 1
        fi
        
        echo "📝 CSV 파일: $(pwd)/data.csv"
        echo "🔥 배치 실행 중..."
        ./gradlew bootRun --args='--spring.batch.job.name=chunkProcessingJob inputFile=data.csv chunkSize=5'
        ;;
        
    *)
        echo "❌ 잘못된 선택"
        exit 1
        ;;
esac
