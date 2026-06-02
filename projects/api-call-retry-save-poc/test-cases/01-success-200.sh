#!/bin/bash
# 1. 성공 케이스 (200)

curl -X POST "http://localhost:8080/api/call/v2" \
  -H "Content-Type: application/json" \
  -d '{"url": "https://httpbin.org/status/200"}' \
  -w "\nStatus: %{http_code}\n"