#!/bin/bash
# 6. 4xx 에러 케이스 (404)

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/404"}' \
  -w "\nStatus: %{http_code}\n"
