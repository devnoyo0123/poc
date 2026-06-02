#!/bin/bash
# 5. 4xx 에러 케이스 (400)

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/400"}' \
  -w "\nStatus: %{http_code}\n"
