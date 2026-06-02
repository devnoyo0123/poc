#!/bin/bash
# 4. 5xx 에러 케이스 (504)

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/504"}' \
  -w "\nStatus: %{http_code}\n"
