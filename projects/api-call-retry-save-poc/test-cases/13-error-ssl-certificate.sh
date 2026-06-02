#!/bin/bash
# 13. SSL 인증서 에러 (SSL Certificate Error) - 만료된/자체 서명된 인증서

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://expired.badssl.com/"}' \
  -w "\nStatus: %{http_code}\n"
