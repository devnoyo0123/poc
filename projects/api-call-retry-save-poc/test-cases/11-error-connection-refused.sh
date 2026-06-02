#!/bin/bash
# 11. 연결 거부 (Connection Refused) - 존재하지 않는 포트

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "http://localhost:9999/"}' \
  -w "\nStatus: %{http_code}\n"
