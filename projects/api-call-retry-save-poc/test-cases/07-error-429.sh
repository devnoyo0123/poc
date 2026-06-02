#!/bin/bash
# 7. 4xx 에러 케이스 (429 - Too Many Requests)

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/429"}' \
  -w "\nStatus: %{http_code}\n"
