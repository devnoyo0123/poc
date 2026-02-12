#!/bin/bash

# Run SSE Performance Benchmark
# This script executes k6 load tests and collects results

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESULTS_DIR="$PROJECT_ROOT/results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Create results directory
mkdir -p "$RESULTS_DIR"

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}  SSE Performance Benchmark${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Function to check if services are running
check_services() {
    echo -e "${YELLOW}Checking if services are running...${NC}"

    if ! curl -s -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
        echo -e "${RED}✗ Servlet Stack is not running!${NC}"
        echo -e "${YELLOW}Run ./scripts/start-all.sh first${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Servlet Stack is running${NC}"

    if ! curl -s -f http://localhost:8082/actuator/health > /dev/null 2>&1; then
        echo -e "${RED}✗ WebFlux Stack is not running!${NC}"
        echo -e "${YELLOW}Run ./scripts/start-all.sh first${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ WebFlux Stack is running${NC}"

    if ! curl -s -f http://localhost:9090/-/healthy > /dev/null 2>&1; then
        echo -e "${RED}✗ Prometheus is not running!${NC}"
        echo -e "${YELLOW}Run ./scripts/start-all.sh first${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Prometheus is running${NC}"

    echo ""
}

# Function to run a single test scenario
run_test() {
    local test_file=$1
    local test_name=$2

    echo -e "${BLUE}Running: $test_name${NC}"
    echo -e "${YELLOW}Test file: $test_file${NC}"
    echo ""

    if k6 run "$test_file" --out json="$RESULTS_DIR/${test_name}_$TIMESTAMP.json"; then
        echo -e "${GREEN}✓ $test_name completed${NC}"
    else
        echo -e "${RED}✗ $test_name failed${NC}"
    fi
    echo ""
}

# Function to collect metrics from Prometheus
collect_metrics() {
    local output_file=$1

    echo -e "${YELLOW}Collecting metrics from Prometheus...${NC}"

    # Collect key metrics at the end of test
    cat > "$output_file" << EOF
# SSE Performance Comparison - Metrics Summary
# Generated at: $(date)

## Active Connections
$(curl -s 'http://localhost:9090/api/v1/query?query=sse_servlet_connections_active' | jq '.data.result[0]')
$(curl -s 'http://localhost:9090/api/v1/query?query=sse_webflux_connections_active' | jq '.data.result[0]')

## Thread Counts
$(curl -s 'http://localhost:9090/api/v1/query?query=jvm_threads_live_threads{application=\"servlet-stack\"}' | jq '.data.result[0]')
$(curl -s 'http://localhost:9090/api/v1/query?query=jvm_threads_live_threads{application=\"webflux-stack\"}' | jq '.data.result[0]')

## Memory Usage (Heap)
$(curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes{application=\"servlet-stack\",area=\"heap\"}' | jq '.data.result[0]')
$(curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes{application=\"webflux-stack\",area=\"heap\"}' | jq '.data.result[0]')

## CPU Usage
$(curl -s 'http://localhost:9090/api/v1/query?query=system_cpu_usage' | jq '.data.result[]')

## Throughput (prices/sec)
$(curl -s 'http://localhost:9090/api/v1/query?query=rate(sse_prices_generated_total[1m])' | jq '.data.result[0]')
EOF

    echo -e "${GREEN}✓ Metrics collected${NC}"
    echo ""
}

# Check services
check_services

# Ask which test to run
echo -e "${BLUE}Select benchmark scenario:${NC}"
echo ""
echo "1) C100  - 100 connections"
echo "2) C1K   - 1,000 connections"
echo "3) C5K   - 5,000 connections"
echo "4) C10K  - 10,000 connections (Main Performance Test)"
echo "5) ALL   - Run all tests sequentially"
echo ""
read -p "Enter choice (1-5): " choice

case $choice in
    1)
        run_test "$PROJECT_ROOT/load-test/k6/scenarios/c100.js" "c100"
        collect_metrics "$RESULTS_DIR/metrics_c100_$TIMESTAMP.txt"
        ;;
    2)
        run_test "$PROJECT_ROOT/load-test/k6/scenarios/c1k.js" "c1k"
        collect_metrics "$RESULTS_DIR/metrics_c1k_$TIMESTAMP.txt"
        ;;
    3)
        run_test "$PROJECT_ROOT/load-test/k6/scenarios/c5k.js" "c5k"
        collect_metrics "$RESULTS_DIR/metrics_c5k_$TIMESTAMP.txt"
        ;;
    4)
        run_test "$PROJECT_ROOT/load-test/k6/scenarios/c10k.js" "c10k"
        collect_metrics "$RESULTS_DIR/metrics_c10k_$TIMESTAMP.txt"
        ;;
    5)
        echo -e "${YELLOW}Running all tests sequentially...${NC}"
        echo ""

        run_test "$PROJECT_ROOT/load-test/k6/scenarios/c100.js" "c100"
        sleep 10

        run_test "$PROJECT_ROOT/load-test/k6/scenarios/c1k.js" "c1k"
        sleep 10

        run_test "$PROJECT_ROOT/load-test/k6/scenarios/c5k.js" "c5k"
        sleep 10

        run_test "$PROJECT_ROOT/load-test/k6/scenarios/c10k.js" "c10k"
        collect_metrics "$RESULTS_DIR/metrics_all_$TIMESTAMP.txt"
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

# Display results
echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}  Benchmark Complete!${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""
echo -e "${GREEN}Results saved to: $RESULTS_DIR${NC}"
echo ""
echo -e "${BLUE}View results in Grafana:${NC}"
echo -e "http://localhost:3000/d/sse-comparison"
echo ""
echo -e "${BLUE}Compare thread counts in Prometheus:${NC}"
echo -e "jvm_threads_live_threads{application=\"servlet-stack\"}"
echo -e "jvm_threads_live_threads{application=\"webflux-stack\"}"
echo ""
