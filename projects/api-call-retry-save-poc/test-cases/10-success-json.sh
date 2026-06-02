#!/bin/bash
# 10. JSON 응답 테스트 (성공)

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/get"}' \
  | jq '.'
