#!/bin/bash
# 12. DNS 해결 실패 (DNS Resolution Failed) - 존재하지 않는 도메인

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://non-existent-domain-12345.invalid"}' \
  -w "\nStatus: %{http_code}\n"
