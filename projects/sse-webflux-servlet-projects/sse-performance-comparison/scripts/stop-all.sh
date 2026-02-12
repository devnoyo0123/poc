#!/bin/bash

# Stop all services for SSE Performance Comparison POC
# This script stops all running services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  SSE Performance Comparison POC - Stop All${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Stop Java processes
if [ -f /tmp/servlet-stack.pid ]; then
    SERVLET_PID=$(cat /tmp/servlet-stack.pid)
    echo -e "${YELLOW}Stopping Servlet Stack (PID: $SERVLET_PID)...${NC}"
    kill $SERVLET_PID 2>/dev/null || true
    rm /tmp/servlet-stack.pid
    echo -e "${GREEN}✓ Servlet Stack stopped${NC}"
fi

if [ -f /tmp/webflux-stack.pid ]; then
    WEBFLUX_PID=$(cat /tmp/webflux-stack.pid)
    echo -e "${YELLOW}Stopping WebFlux Stack (PID: $WEBFLUX_PID)...${NC}"
    kill $WEBFLUX_PID 2>/dev/null || true
    rm /tmp/webflux-stack.pid
    echo -e "${GREEN}✓ WebFlux Stack stopped${NC}"
fi

# Kill any remaining Java processes on ports 8081 and 8082
echo -e "${YELLOW}Checking for remaining Java processes...${NC}"
for port in 8081 8082; do
    pid=$(lsof -ti :$port 2>/dev/null || true)
    if [ -n "$pid" ]; then
        echo -e "${YELLOW}Killing process on port $port (PID: $pid)...${NC}"
        kill -9 $pid 2>/dev/null || true
    fi
done

# Stop Docker Compose
echo ""
echo -e "${YELLOW}Stopping Docker Compose...${NC}"
cd "$PROJECT_ROOT/docker"
docker-compose down
echo -e "${GREEN}✓ Docker Compose stopped${NC}"

echo ""
echo -e "${GREEN}All services stopped successfully!${NC}"
