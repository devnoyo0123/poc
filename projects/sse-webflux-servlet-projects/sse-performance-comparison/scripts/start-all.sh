#!/bin/bash

# Start all services for SSE Performance Comparison POC
# This script starts Prometheus, Grafana, Servlet Stack, and WebFlux Stack

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
echo -e "${BLUE}  SSE Performance Comparison POC - Start All${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Function to check if port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local name=$2
    local max_attempts=30
    local attempt=1

    echo -e "${YELLOW}Waiting for $name to be ready...${NC}"
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ $name is ready!${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    echo ""
    echo -e "${RED}✗ $name failed to start${NC}"
    return 1
}

# Step 1: Start Docker Compose (Prometheus, Grafana)
echo -e "${BLUE}[1/4] Starting Docker Compose (Prometheus, Grafana)...${NC}"
cd "$PROJECT_ROOT/docker"
if ! docker-compose up -d; then
    echo -e "${RED}Failed to start Docker Compose${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker Compose started${NC}"
echo ""

# Wait for Prometheus
wait_for_service "http://localhost:9090/-/healthy" "Prometheus"

# Wait for Grafana
wait_for_service "http://localhost:3000/api/health" "Grafana"

# Step 2: Start Servlet Stack
echo ""
echo -e "${BLUE}[2/4] Starting Servlet Stack (port 8081)...${NC}"
if check_port 8081; then
    echo -e "${YELLOW}Port 8081 already in use, skipping Servlet Stack${NC}"
else
    cd "$PROJECT_ROOT"
    ./gradlew :servlet-stack:bootRun \
        --args='--spring.profiles.active=servlet' \
        > logs/servlet-stack.log 2>&1 &
    SERVLET_PID=$!
    echo $SERVLET_PID > /tmp/servlet-stack.pid
    echo -e "${GREEN}✓ Servlet Stack started (PID: $SERVLET_PID)${NC}"

    wait_for_service "http://localhost:8081/actuator/health" "Servlet Stack"
fi

# Step 3: Start WebFlux Stack
echo ""
echo -e "${BLUE}[3/4] Starting WebFlux Stack (port 8082)...${NC}"
if check_port 8082; then
    echo -e "${YELLOW}Port 8082 already in use, skipping WebFlux Stack${NC}"
else
    cd "$PROJECT_ROOT"
    ./gradlew :webflux-stack:bootRun \
        --args='--spring.profiles.active=webflux' \
        > logs/webflux-stack.log 2>&1 &
    WEBFLUX_PID=$!
    echo $WEBFLUX_PID > /tmp/webflux-stack.pid
    echo -e "${GREEN}✓ WebFlux Stack started (PID: $WEBFLUX_PID)${NC}"

    wait_for_service "http://localhost:8082/actuator/health" "WebFlux Stack"
fi

# Step 4: Display service URLs
echo ""
echo -e "${BLUE}[4/4] All services started successfully!${NC}"
echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}  Service URLs${NC}"
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}Servlet Stack:${NC}      http://localhost:8081"
echo -e "${GREEN}  - Actuator:${NC}        http://localhost:8081/actuator"
echo -e "${GREEN}  - Metrics:${NC}         http://localhost:8081/actuator/prometheus"
echo -e "${GREEN}  - SSE Stream:${NC}      http://localhost:8081/api/prices/stream"
echo ""
echo -e "${GREEN}WebFlux Stack:${NC}       http://localhost:8082"
echo -e "${GREEN}  - Actuator:${NC}        http://localhost:8082/actuator"
echo -e "${GREEN}  - Metrics:${NC}         http://localhost:8082/actuator/prometheus"
echo -e "${GREEN}  - SSE Stream:${NC}      http://localhost:8082/api/prices/stream"
echo ""
echo -e "${GREEN}Prometheus:${NC}          http://localhost:9090"
echo -e "${GREEN}Grafana:${NC}             http://localhost:3000"
echo -e "${GREEN}  (admin/admin)${NC}"
echo ""
echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  Quick Test Commands${NC}"
echo -e "${BLUE}================================================${NC}"
echo -e "${YELLOW}# Test Servlet SSE${NC}"
echo -e "curl -N http://localhost:8081/api/prices/stream"
echo ""
echo -e "${YELLOW}# Test WebFlux SSE${NC}"
echo -e "curl -N http://localhost:8082/api/prices/stream"
echo ""
echo -e "${YELLOW}# Run benchmark${NC}"
echo -e "./scripts/benchmark.sh"
echo ""
