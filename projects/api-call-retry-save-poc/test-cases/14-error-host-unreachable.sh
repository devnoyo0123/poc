#!/bin/bash
# 14. 호스트 도달 불가 (Host Unreachable) - 존재하지 않는 사설 IP

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://10.255.255.1/"}' \
  -w "\nStatus: %{http_code}\n"
