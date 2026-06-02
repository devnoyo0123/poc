#!/bin/bash
# 9. 타임아웃 케이스 (5초 지연)

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/delay/5"}' \
  -w "\nStatus: %{http_code}\n"
