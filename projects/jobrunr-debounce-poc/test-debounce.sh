#!/bin/bash
set -e

KEY="${1:-2024-01}"
DELAY="${2:-3}"
COUNT="${3:-3}"

echo "=========================================="
echo " JobRunr Debounce Test"
echo " Key: $KEY | Delay: ${DELAY}s | Count: $COUNT"
echo "=========================================="
echo ""

for i in $(seq 1 "$COUNT"); do
    echo "--- Trigger $i at $(date '+%H:%M:%S') ---"
    RESPONSE=$(curl -s -X POST "localhost:8080/api/trigger/$KEY")
    RUN_AT=$(echo "$RESPONSE" | grep -o '"runAt":"[^"]*"' | cut -d'"' -f4)
    JOB_ID=$(echo "$RESPONSE" | grep -o '"jobId":"[^"]*"' | cut -d'"' -f4)
    echo "  jobId: ${JOB_ID:0:8}... | runAt: $RUN_AT"
    [ "$i" -lt "$COUNT" ] && sleep "$DELAY"
done

echo ""
echo "All triggers sent. Waiting for debounce window..."
echo "Expected: job executes ONCE ~$(date -v+${DELAY}S '+%H:%M:%S' 2>/dev/null || date -d "+${DELAY} seconds" '+%H:%M:%S' 2>/dev/null)"
echo ""
echo "Press Ctrl+C to stop watching logs."
echo "=========================================="
