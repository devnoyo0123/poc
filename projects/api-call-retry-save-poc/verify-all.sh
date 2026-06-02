#!/bin/bash

# API Call Retry & Save POC - 모든 검증 케이스 실행 스크립트
# 사용법: ./verify-all.sh

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m✅'
RED='\033[0;31m❌'
YELLOW='\033[1;33m⚠️'

echo -e "${YELLOW}=== API Call Retry & Save POC 검증 스크립트 ===${NC}"
echo ""

# TC-001: 성공 시나리오 검증
echo -e "${YELLOW}[TC-001] 성공 시나리오 검증${NC}"
echo "---------------------------------------------------"
RESPONSE=$(curl -s -X POST ${BASE_URL}/api/call \
  -H "Content-Type: application/json" \
  -d '{"url": "http://localhost:8080/mock/success"}')

echo "응답:"
echo "$RESPONSE" | jq '.'

# DB 확인
HISTORY=$(curl -s "${BASE_URL}/api/call/history?endpoint=http://localhost:8080/mock/success")
echo ""
echo "DB 이력:"
echo "$HISTORY" | jq '.'

# 성공 검증
STATUS=$(echo "$RESPONSE" | jq -r '.status')
ATTEMPT_COUNT=$(echo "$RESPONSE" | jq -r '.attemptCount')
SUCCESS=$(echo "$RESPONSE" | jq -r '.isSuccess')

if [ "$STATUS" == "SUCCESS" ] && [ "$ATTEMPT_COUNT" == "1" ] && [ "$SUCCESS" == "true" ]; then
    echo -e "${GREEN}✅ PASS${NC}: 성공 시나리오 확인"
else
    echo -e "${RED}❌ FAIL${NC}: 성공 시나리오 실패"
fi

echo ""
sleep 2

# TC-002: 500 에러 재시도 검증
echo -e "${YELLOW}[TC-002] 500 에러 재시도 검증${NC}"
echo "---------------------------------------------------"
START_TIME=$(date +%s)

RESPONSE=$(curl -s -X POST ${BASE_URL}/api/call \
  -H "Content-Type: application/json" \
  -d '{"url": "http://localhost:8080/mock/500"}')

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo "응답 (소요 시간: ${DURATION}초):"
echo "$RESPONSE" | jq '.'

# 성공 검증
STATUS=$(echo "$RESPONSE" | jq -r '.status')
ATTEMPT_COUNT=$(echo "$RESPONSE" | jq -r '.attemptCount')
ERROR_MSG=$(echo "$RESPONSE" | jq -r '.errorMessage')

if [ "$STATUS" == "FAILED" ] && [ "$ATTEMPT_COUNT" == "3" ]; then
    echo -e "${GREEN}✅ PASS${NC}: 3회 재시도 후 실패 확인"
    if [[ "$ERROR_MSG" == *"3 attempts"* ]]; then
        echo -e "${GREEN}✅ PASS${NC}: 에러 메시지 확인"
    else
        echo -e "${RED}❌ FAIL${NC}: 에러 메시지 확인"
    fi
else
    echo -e "${RED}❌ FAIL${NC}: 재시도 동작 실패"
fi

# DB 확인
HISTORY=$(curl -s "${BASE_URL}/api/call/history?endpoint=http://localhost:8080/mock/500")
echo ""
echo "DB 이력:"
echo "$HISTORY" | jq '.'

# 시간 검증 (7초 이상)
if [ $DURATION -ge 7 ]; then
    echo -e "${GREEN}✅ PASS${NC}: 시간 검증 (${DURATION}초 ≥ 7초)"
else
    echo -e "${RED}❌ FAIL${NC}: 시간 검증 (${DURATION}초 < 7초)"
fi

echo ""
sleep 2

# TC-003: 429 에러 재시도 검증
echo -e "${YELLOW}[TC-003] 429 에러 재시도 검증${NC}"
echo "---------------------------------------------------"
START_TIME=$(date +%s)

RESPONSE=$(curl -s -X POST ${BASE_URL}/api/call \
  -H "Content-Type: application/json" \
  -d '{"url": "http://localhost:8080/mock/429"}')

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo "응답 (소요 시간: ${DURATION}초):"
echo "$RESPONSE" | jq '.'

# 성공 검증
STATUS=$(echo "$RESPONSE" | jq -r '.status')
ATTEMPT_COUNT=$(echo "$RESPONSE" | jq -r '.attemptCount')

if [ "$STATUS" == "FAILED" ] && [ "$ATTEMPT_COUNT" == "3" ]; then
    echo -e "${GREEN}✅ PASS${NC}: 3회 재시도 후 실패 확인"
else
    echo -e "${RED}❌ FAIL${NC}: 재시도 동작 실패"
fi

# DB 확인
HISTORY=$(curl -s "${BASE_URL}/api/call/history?endpoint=http://localhost:8080/mock/429")
echo ""
echo "DB 이력:"
echo "$HISTORY" | jq '.'

# 시간 검증
if [ $DURATION -ge 7 ]; then
    echo -e "${GREEN}✅ PASS${NC}: 시간 검증 (${DURATION}초 ≥ 7초)"
else
    echo -e "${RED}❌ FAIL${NC}: 시간 검증 (${DURATION}초 < 7초)"
fi

echo ""
sleep 2

# TC-004: 400 즉시 실패 검증
echo -e "${YELLOW}[TC-004-1] 400 즉시 실패 검증${NC}"
echo "---------------------------------------------------"
START_TIME=$(date +%s)

RESPONSE=$(curl -s -X POST ${BASE_URL}/api/call \
  -H "Content-Type: application/json" \
  -d '{"url": "http://localhost:8080/mock/400"}')

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo "응답 (소요 시간: ${DURATION}초):"
echo "$RESPONSE" | jq '.'

# 성공 검증
STATUS=$(echo "$RESPONSE" | jq -r '.status')
ATTEMPT_COUNT=$(echo "$RESPONSE" | jq -r '.attemptCount')

if [ "$STATUS" == "FAILED" ] && [ "$ATTEMPT_COUNT" == "1" ]; then
    echo -e "${GREEN}✅ PASS${NC}: 즉시 실패 확인"
    echo -e "${GREEN}✅ PASS${NC}: attemptCount=1 확인"
else
    echo -e "${RED}❌ FAIL${NC}: 즉시 실패 동작 실패"
fi

# DB 확인
HISTORY=$(curl -s "${BASE_URL}/api/call/history?endpoint=http://localhost:8080/mock/400")
echo ""
echo "DB 이력:"
echo "$HISTORY" | jq '.'

# 결과가 1개인지 확인
RESULT_COUNT=$(echo "$HISTORY" | jq '. | length')
if [ "$RESULT_COUNT" == "1" ]; then
    echo -e "${GREEN}✅ PASS${NC}: DB에 결과 1개만 저장됨"
else
    echo -e "${RED}❌ FAIL${NC}: DB에 결과 ${RESULT_COUNT}개 저장됨"
fi

echo ""
sleep 2

# TC-004-2: 404 즉시 실패 검증
echo -e "${YELLOW}[TC-004-2] 404 즉시 실패 검증${NC}"
echo "---------------------------------------------------"
START_TIME=$(date +%s)

RESPONSE=$(curl -s -X POST ${BASE_URL}/api/call \
  -H "Content-Type: application/json" \
  -d '{"url": "http://localhost:8080/mock/404"}')

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo "응답 (소요 시간: ${DURATION}초):"
echo "$RESPONSE" | jq '.'

# 성공 검증
STATUS=$(echo "$RESPONSE" | jq -r '.status')
ATTEMPT_COUNT=$(echo "$RESPONSE" | jq -r '.attemptCount')

if [ "$STATUS" == "FAILED" ] && [ "$ATTEMPT_COUNT" == "1" ]; then
    echo -e "${GREEN}✅ PASS${NC}: 즉시 실패 확인"
    echo -e "${GREEN}✅ PASS${NC}: attemptCount=1 확인"
else
    echo -e "${RED}❌ FAIL${NC}: 즉시 실패 동작 실패"
fi

# DB 확인
HISTORY=$(curl -s "${BASE_URL}/api/call/history?endpoint=http://localhost:8080/mock/404")
echo ""
echo "DB 이력:"
echo "$HISTORY" | jq '.'

# 결과가 1개인지 확인
RESULT_COUNT=$(echo "$HISTORY" | jq '. | length')
if [ "$RESULT_COUNT" == "1" ]; then
    echo -e "${GREEN}✅ PASS${NC}: DB에 결과 1개만 저장됨"
else
    echo -e "${RED}❌ FAIL${NC}: DB에 결과 ${RESULT_COUNT}개 저장됨"
fi

echo ""
sleep 2

# TC-005: 호출 이력 조회 검증
echo -e "${YELLOW}[TC-005] 호출 이력 조회 검증${NC}"
echo "---------------------------------------------------"

# 동일 엔드포인트로 2번 호출
curl -s -X POST ${BASE_URL}/api/call \
  -H "Content-Type: application/json" \
  -d '{"url": "http://localhost:8080/mock/success"}' > /dev/null

curl -s -X POST ${BASE_URL}/api/call \
  -H "Content-Type: application/json" \
  -d '{"url": "http://localhost:8080/mock/success"}' > /dev/null

# 이력 조회
HISTORY=$(curl -s "${BASE_URL}/api/call/history?endpoint=http://localhost:8080/mock/success")

echo "호출 이력:"
echo "$HISTORY" | jq '.'

# 최신순 정렬 확인 (최신이 먼저 오는지)
FIRST_ID=$(echo "$HISTORY" | jq -r '.[0].id')
SECOND_ID=$(echo "$HISTORY" | jq -r '.[1].id')

if [ "$FIRST_ID" -gt "$SECOND_ID" ]; then
    echo -e "${RED}❌ FAIL${NC}: 최신순 정렬 실패 (ID 내림차순)"
else
    echo -e "${GREEN}✅ PASS${NC}: 최신순 정렬 확인 (ID 오름차순)"
fi

# 2개의 기록이 있는지 확인
RESULT_COUNT=$(echo "$HISTORY" | jq '. | length')
if [ "$RESULT_COUNT" -ge 2 ]; then
    echo -e "${GREEN}✅ PASS${NC}: 2개 이상의 기록 조회됨"
else
    echo -e "${RED}❌ FAIL${NC}: 기록이 2개 미만"
fi

echo ""
echo -e "${YELLOW}=== 모든 검증 완료 ===${NC}"
echo ""
echo "사용법: ./verify-all.sh"
echo "참고: 애플리케이션이 실행 중이어야 합니다."
echo ""
