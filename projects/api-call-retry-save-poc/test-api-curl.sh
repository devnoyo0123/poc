#!/bin/bash

# API 호출 테스트 스크립트
# 각 케이스별로 테스트 URL과 curl 명령어 정의

BASE_URL="http://localhost:8080/api/call"

echo "========================================="
echo "API Call 테스트 (버전 2)"
echo "========================================="
echo ""

# 1. 성공 케이스 (200)
echo "=== 1. 성공 케이스 (200) ==="
curl -X POST "${BASE_URL}/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/200"}' \
  -w "\nStatus: %{http_code}\n" \
  -s
echo -e "\n"
sleep 1

# 2. 5xx 에러 케이스 (500)
echo "=== 2. 5xx 에러 케이스 (500) ==="
curl -X POST "${BASE_URL}/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/500"}' \
  -w "\nStatus: %{http_code}\n" \
  -s
echo -e "\n"
sleep 1

# 3. 5xx 에러 케이스 (502)
echo "=== 3. 5xx 에러 케이스 (502) ==="
curl -X POST "${BASE_URL}/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/502"}' \
  -w "\nStatus: %{http_code}\n" \
  -s
echo -e "\n"
sleep 1

# 4. 5xx 에러 케이스 (504)
echo "=== 4. 5xx 에러 케이스 (504) ==="
curl -X POST "${BASE_URL}/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/504"}' \
  -w "\nStatus: %{http_code}\n" \
  -s
echo -e "\n"
sleep 1

# 5. 4xx 에러 케이스 (400)
echo "=== 5. 4xx 에러 케이스 (400) ==="
curl -X POST "${BASE_URL}/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/400"}' \
  -w "\nStatus: %{http_code}\n" \
  -s
echo -e "\n"
sleep 1

# 6. 4xx 에러 케이스 (404)
echo "=== 6. 4xx 에러 케이스 (404) ==="
curl -X POST "${BASE_URL}/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/404"}' \
  -w "\nStatus: %{http_code}\n" \
  -s
echo -e "\n"
sleep 1

# 7. 4xx 에러 케이스 (429)
echo "=== 7. 4xx 에러 케이스 (429) ==="
curl -X POST "${BASE_URL}/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/429"}' \
  -w "\nStatus: %{http_code}\n" \
  -s
echo -e "\n"
sleep 1

# 8. 기타 에러 케이스 (잘못된 URL)
echo "=== 8. 기타 에러 케이스 (잘못된 URL) ==="
curl -X POST "${BASE_URL}/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://this-domain-does-not-exist-12345.com"}' \
  -w "\nStatus: %{http_code}\n" \
  -s
echo -e "\n"
sleep 1

# 9. 타임아웃 케이스
echo "=== 9. 타임아웃 케이스 (5초 지연) ==="
curl -X POST "${BASE_URL}/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/delay/5"}' \
  -w "\nStatus: %{http_code}\n" \
  -s
echo -e "\n"

echo "========================================="
echo "테스트 완료"
echo "========================================="