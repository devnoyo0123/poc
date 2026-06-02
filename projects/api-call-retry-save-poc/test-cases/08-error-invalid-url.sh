#!/bin/bash
# 8. 기타 에러 케이스 (잘못된 URL)

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://this-domain-does-not-exist-12345.com"}' \
  -w "\nStatus: %{http_code}\n"
