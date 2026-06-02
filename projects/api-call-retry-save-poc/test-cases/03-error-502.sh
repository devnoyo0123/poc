#!/bin/bash
# 3. 5xx 에러 케이스 (502)

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/502"}' \
  -w "\nStatus: %{http_code}\n"
