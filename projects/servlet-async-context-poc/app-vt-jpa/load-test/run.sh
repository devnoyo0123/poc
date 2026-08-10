#!/bin/bash
# =============================================================================
# VT + JPA POC - 테스트 러너
# 사용법:
#   ./run.sh start                       # 앱 백그라운드 시작 (빌드 포함)
#   ./run.sh stop                        # 앱 중지
#   ./run.sh status                      # 앱 상태 확인
#   ./run.sh single                      # /jpa/health, /jpa/count 단일 호출
#   ./run.sh bench-sequential [N]        # /jpa/sequential N 동시 (기본 50)
#   ./run.sh bench-parallel-platform [N] # /jpa/parallel-platform N 동시
#   ./run.sh bench-parallel-virtual [N]  # /jpa/parallel-virtual N 동시
#   ./run.sh bench-async-fanout [N]      # /jpa/async-fanout N 동시
#   ./run.sh compare-parallel [N]        # sequential vs parallel-platform vs parallel-virtual
#   ./run.sh compare-concurrency [N]     # N=10,50,100 변화 (기본 50)
#   ./run.sh pinning-trace               # pinning 추적 안내
#   ./run.sh all                         # 전체 시나리오 순차 실행
# =============================================================================

set -euo pipefail

# ==================== 설정 ====================
PORT="${PORT:-8082}"
BASE="http://localhost:${PORT}"
APP_JAR="build/libs/app-vt-jpa.jar"
APP_LOG="/tmp/vt-jpa-poc.log"
APP_PID_FILE="/tmp/vt-jpa-poc.pid"
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPO_ROOT="$(cd "$PROJECT_ROOT/.." && pwd)"

# 색상
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# ==================== 유틸 ====================
log()   { echo -e "${CYAN}[$(date +%H:%M:%S)]${NC} $*"; }
ok()    { echo -e "${GREEN}✓${NC} $*"; }
warn()  { echo -e "${YELLOW}!${NC} $*"; }
err()   { echo -e "${RED}✗${NC} $*" >&2; }
header(){ echo -e "\n${BOLD}${BLUE}═══ $* ═══${NC}\n"; }

wait_for_app() {
    local max=60
    for i in $(seq 1 $max); do
        if curl -s -o /dev/null -m 1 "$BASE/jpa/health" 2>/dev/null; then
            return 0
        fi
        sleep 1
        printf "."
    done
    echo
    return 1
}

require_app() {
    if ! curl -s -o /dev/null -m 2 "$BASE/jpa/health" 2>/dev/null; then
        err "앱이 $BASE 에서 응답하지 않습니다."
        warn "먼저 실행: ./run.sh start"
        exit 1
    fi
}

# ==================== 앱 제어 ====================
cmd_start() {
    header "앱 시작"
    cd "$REPO_ROOT"

    if [ -f "$APP_PID_FILE" ] && kill -0 "$(cat $APP_PID_FILE)" 2>/dev/null; then
        warn "이미 실행 중 (PID=$(cat $APP_PID_FILE))"
        exit 0
    fi

    if [ ! -f "$PROJECT_ROOT/$APP_JAR" ]; then
        log "JAR 빌드 중... (:app-vt-jpa:bootJar)"
        ./gradlew :app-vt-jpa:bootJar -q
    fi

    log "앱 기동 (포트=$PORT)"
    nohup java -jar "$PROJECT_ROOT/$APP_JAR" --server.port="$PORT" > "$APP_LOG" 2>&1 &
    echo $! > "$APP_PID_FILE"
    local pid=$(cat $APP_PID_FILE)

    printf "대기 중"
    if ! wait_for_app; then
        err "앱 시작 실패. 로그 확인: $APP_LOG"
        tail -30 "$APP_LOG"
        exit 1
    fi
    echo
    ok "실행 중 (PID=$pid, port=$PORT)"
}

cmd_stop() {
    header "앱 중지"
    if [ ! -f "$APP_PID_FILE" ]; then
        warn "PID 파일 없음. 이미 중지됐을 수 있음."
        exit 0
    fi
    local pid=$(cat $APP_PID_FILE)
    if kill -0 "$pid" 2>/dev/null; then
        kill "$pid"
        sleep 2
        if kill -0 "$pid" 2>/dev/null; then
            warn "강제 종료"
            kill -9 "$pid"
        fi
        ok "중지됨 (PID=$pid)"
    else
        warn "프로세스 없음"
    fi
    rm -f "$APP_PID_FILE"
}

cmd_status() {
    header "상태"
    if [ -f "$APP_PID_FILE" ] && kill -0 "$(cat $APP_PID_FILE)" 2>/dev/null; then
        ok "실행 중 (PID=$(cat $APP_PID_FILE), port=$PORT)"
        if curl -s -o /dev/null -m 2 "$BASE/jpa/health"; then
            ok "HTTP 응답 정상"
        fi
    else
        err "중지됨"
    fi
}

# ==================== 부하 측정 ====================
# 인자: 엔드포인트, 동시성, 총 요청 수, 라벨
# 결과: 전역 변수 RES_* 에 저장
run_load() {
    local endpoint="$1"
    local concurrency="$2"
    local total="$3"
    local label="$4"

    local url="$BASE$endpoint"
    local tmpfile
    tmpfile=$(mktemp)

    seq 1 "$total" | xargs -I{} -P "$concurrency" \
        curl -s -o /dev/null -w "%{time_total}\n" "$url" > "$tmpfile" 2>/dev/null

    awk -v label="$label" '
        BEGIN { min=999999; max=0; sum=0; count=0 }
        { sum+=$1; count++; if($1<min)min=$1; if($1>max)max=$1 }
        END {
            avg = sum/count
            wall = max
            rps = count/wall
            printf "LABEL=%s\nCOUNT=%d\nMIN=%.3f\nAVG=%.3f\nMAX=%.3f\nWALL=%.3f\nRPS=%.1f\n",
                   label, count, min, avg, max, wall, rps
        }
    ' "$tmpfile"
    rm -f "$tmpfile"
}

parse_result() {
    local output="$1"
    RES_LABEL=$(echo "$output" | grep '^LABEL=' | cut -d= -f2)
    RES_COUNT=$(echo "$output" | grep '^COUNT=' | cut -d= -f2)
    RES_MIN=$(echo  "$output" | grep '^MIN='   | cut -d= -f2)
    RES_AVG=$(echo  "$output" | grep '^AVG='   | cut -d= -f2)
    RES_MAX=$(echo  "$output" | grep '^MAX='   | cut -d= -f2)
    RES_WALL=$(echo "$output" | grep '^WALL='  | cut -d= -f2)
    RES_RPS=$(echo  "$output" | grep '^RPS='   | cut -d= -f2)
}

print_summary() {
    echo
    echo -e "${BOLD}─────────── 결과 ───────────${NC}"
    printf "%-15s %s\n"   "패턴"        "$RES_LABEL"
    printf "%-15s %d\n"   "요청 수"      "$RES_COUNT"
    printf "%-15s %ss\n"  "Wall 시간"   "$RES_WALL"
    printf "%-15s %ss\n"  "최소 지연"   "$RES_MIN"
    printf "%-15s %ss\n"  "평균 지연"   "$RES_AVG"
    printf "%-15s %ss\n"  "최대 지연"   "$RES_MAX"
    printf "%-15s %s rps\n" "Throughput" "$RES_RPS"
    echo
}

# ==================== 명령어 ====================
cmd_single() {
    require_app
    header "단일 요청 (정상 점검)"

    echo -e "${BOLD}/jpa/health${NC} 호출"
    local h_body=$(curl -s "$BASE/jpa/health")
    echo -e "  응답: ${CYAN}${h_body}${NC}"

    echo
    echo -e "${BOLD}/jpa/count${NC} 호출"
    local c_body=$(curl -s "$BASE/jpa/count")
    echo -e "  응답: ${CYAN}${c_body}${NC}"

    echo
    echo -e "${BOLD}/jpa/sequential${NC} 호출 (약 2초 소요)"
    local s_time=$(curl -s -o /dev/null -w "%{time_total}" "$BASE/jpa/sequential")
    echo -e "  응답 시간: ${YELLOW}${s_time}s${NC}"

    echo
    echo -e "${BOLD}/jpa/parallel-virtual${NC} 호출 (약 1초 소요)"
    local v_time=$(curl -s -o /dev/null -w "%{time_total}" "$BASE/jpa/parallel-virtual")
    echo -e "  응답 시간: ${YELLOW}${v_time}s${NC}"

    echo
    ok "sequential ≈ 2s / parallel-* ≈ 1s 면 정상"
}

cmd_bench() {
    require_app
    local endpoint="$1"
    local n="${2:-50}"
    local label="$3"
    header "${label} / 동시 ${n}"
    log "실행 중..."

    local result
    result=$(run_load "$endpoint" "$n" "$n" "$label")
    parse_result "$result"
    print_summary
}

cmd_bench_sequential()       { cmd_bench "/jpa/sequential"        "${1:-50}" "SEQUENTIAL"; }
cmd_bench_parallel_platform() { cmd_bench "/jpa/parallel-platform" "${1:-50}" "PARALLEL_PLATFORM"; }
cmd_bench_parallel_virtual()  { cmd_bench "/jpa/parallel-virtual"  "${1:-50}" "PARALLEL_VIRTUAL"; }
cmd_bench_async_fanout()      { cmd_bench "/jpa/async-fanout"      "${1:-50}" "ASYNC_FANOUT"; }

cmd_compare_parallel() {
    require_app
    local n="${1:-50}"
    header "sequential vs parallel-platform vs parallel-virtual / 동시 ${n}"

    local endpoints=("/jpa/sequential" "/jpa/parallel-platform" "/jpa/parallel-virtual")
    local labels=("SEQUENTIAL" "PARALLEL_PLATFORM" "PARALLEL_VIRTUAL")
    local walls=() avgs=() maxes=() rpses=() mins=()

    for i in 0 1 2; do
        echo -e "${BOLD}$((i+1))/3 ${labels[$i]}${NC} 실행 중..."
        local result
        result=$(run_load "${endpoints[$i]}" "$n" "$n" "${labels[$i]}")
        parse_result "$result"
        mins+=("$RES_MIN"); walls+=("$RES_WALL"); avgs+=("$RES_AVG"); maxes+=("$RES_MAX"); rpses+=("$RES_RPS")
    done

    echo
    echo -e "${BOLD}─────────── 결과 ───────────${NC}"
    printf "%-18s %-18s %-18s %-18s\n" "지표" "${labels[0]}" "${labels[1]}" "${labels[2]}"
    printf "%-18s %-18s %-18s %-18s\n" "──────────────" "──────────────" "──────────────" "──────────────"
    printf "%-18s %-18s %-18s %-18s\n" "동시 요청" "$n" "$n" "$n"
    printf "%-18s %-18s %-18s %-18s\n" "Wall 시간" "${walls[0]}s" "${walls[1]}s" "${walls[2]}s"
    printf "%-18s %-18s %-18s %-18s\n" "최소 지연" "${mins[0]}s" "${mins[1]}s" "${mins[2]}s"
    printf "%-18s %-18s %-18s %-18s\n" "평균 지연" "${avgs[0]}s" "${avgs[1]}s" "${avgs[2]}s"
    printf "%-18s %-18s %-18s %-18s\n" "최대 지연" "${maxes[0]}s" "${maxes[1]}s" "${maxes[2]}s"
    printf "%-18s %-18s %-18s %-18s\n" "Throughput" "${rpses[0]} rps" "${rpses[1]} rps" "${rpses[2]} rps"

    echo
    echo -e "${BOLD}─────────── 개선 (vs SEQUENTIAL) ───────────${NC}"
    awk -v sw="${walls[0]}" -v pw="${walls[1]}" -v vw="${walls[2]}" \
        -v sr="${rpses[0]}" -v pr="${rpses[1]}" -v vr="${rpses[2]}" '
        BEGIN {
            printf "Wall 시간:  platform %.2fx / virtual %.2fx 빠름\n", sw/pw, sw/vw
            printf "Throughput: platform %.2fx / virtual %.2fx 증가\n", pr/sr, vr/sr
        }
    '
    echo
}

cmd_compare_concurrency() {
    require_app
    local base_n="${1:-50}"
    header "동시성 변화 / N=10,50,${base_n}"

    local endpoints=("/jpa/sequential" "/jpa/parallel-platform" "/jpa/parallel-virtual" "/jpa/async-fanout")
    local labels=("SEQUENTIAL" "PLATFORM" "VIRTUAL" "ASYNC_FANOUT")
    local concurrencies=(10 50 "$base_n")

    printf "%-18s %-15s %-15s %-15s %-15s\n" \
        "동시성 / 지표" "${labels[0]}" "${labels[1]}" "${labels[2]}" "${labels[3]}"
    printf "%-18s %-15s %-15s %-15s %-15s\n" \
        "─────────────" "─────────────" "─────────────" "─────────────" "─────────────"

    for n in "${concurrencies[@]}"; do
        local row_rps=()
        for i in 0 1 2 3; do
            local result
            result=$(run_load "${endpoints[$i]}" "$n" "$n" "${labels[$i]}")
            parse_result "$result"
            row_rps+=("$RES_RPS")
            echo -e "  ${CYAN}N=${n} ${labels[$i]} done${NC}"
        done
        printf "%-18s %-15s %-15s %-15s %-15s\n" \
            "N=$n throughput" \
            "${row_rps[0]} rps" "${row_rps[1]} rps" "${row_rps[2]} rps" "${row_rps[3]} rps"
    done
    echo
}

cmd_pinning_trace() {
    header "Virtual Thread Pinning 추적"
    cat <<EOF
synchronized 블록 내에서의 virtual thread pinning 감지 방법:

1) 앱 재시작 (pinning 추적 옵션 활성화):
   ${BOLD}./run.sh stop${NC}
   ${BOLD}JAVA_OPTS="-Djdk.tracePinnedThreads=full" ./run.sh start${NC}

   (start 명령이 JAVA_OPTS 를 java -jar 앞에 붙이지 않으므로, 수동 실행 권장)
   ${BOLD}nohup java -Djdk.tracePinnedThreads=full -jar ${APP_JAR} > /tmp/vt-jpa-poc.log 2>&1 &${NC}

2) 부하 발생:
   ${BBIND}./run.sh bench-parallel-virtual 50${NC}

3) pinning 발생 시 로그 확인:
   ${BOLD}grep -A 20 'Thread.*pinned' /tmp/vt-jpa-poc.log${NC}

4) 주요 pinning 후보 (실험 관찰 포인트):
   - HikariCP 커넥션 획득 (synchronized 사용 이력 있음)
   - JDBC 드라이버 내부 (PgConnection 등)
   - logging framework 동기화
EOF
    echo
}

cmd_all() {
    require_app
    header "전체 시나리오 순차 실행"

    log "1/7 단일 요청"
    cmd_single

    log "2/7 bench-sequential 50"
    cmd_bench_sequential 50

    log "3/7 bench-parallel-platform 50"
    cmd_bench_parallel_platform 50

    log "4/7 bench-parallel-virtual 50"
    cmd_bench_parallel_virtual 50

    log "5/7 bench-async-fanout 50"
    cmd_bench_async_fanout 50

    log "6/7 compare-parallel 50"
    cmd_compare_parallel 50

    log "7/7 pinning-trace 안내"
    cmd_pinning_trace
}

# ==================== 도움말 ====================
usage() {
    cat <<EOF
${BOLD}사용법:${NC} ./run.sh <명령> [인자]

${BOLD}앱 제어:${NC}
  start                       앱 백그라운드 시작 (빌드 포함)
  stop                        앱 중지
  status                      앱 실행 상태 확인

${BOLD}단일/벤치:${NC}
  single                      /jpa/health, /jpa/count, /jpa/sequential, /jpa/parallel-virtual 호출
  bench-sequential [N]        /jpa/sequential N 동시 (기본 50)
  bench-parallel-platform [N] /jpa/parallel-platform N 동시 (기본 50)
  bench-parallel-virtual [N]  /jpa/parallel-virtual N 동시 (기본 50)
  bench-async-fanout [N]      /jpa/async-fanout N 동시 (기본 50)

${BOLD}비교:${NC}
  compare-parallel [N]        sequential vs parallel-platform vs parallel-virtual (기본 50)
  compare-concurrency [N]     N=10,50,N 변화하며 각 엔드포인트 throughput 비교

${BOLD}진단:${NC}
  pinning-trace               VT pinning 추적 안내 (재시작 지시)

${BOLD}전체:${NC}
  all                         전체 시나리오 순차 실행

${BOLD}환경 변수:${NC}
  PORT=8082                   앱 포트 (기본 8082)

${BOLD}예시:${NC}
  ./run.sh start
  ./run.sh single
  ./run.sh compare-parallel 50
  ./run.sh compare-concurrency 100
  PORT=9000 ./run.sh start

EOF
}

# ==================== 진입점 ====================
case "${1:-}" in
    start)                   cmd_start ;;
    stop)                    cmd_stop ;;
    status)                  cmd_status ;;
    single)                  cmd_single ;;
    bench-sequential)        cmd_bench_sequential "${2:-50}" ;;
    bench-parallel-platform) cmd_bench_parallel_platform "${2:-50}" ;;
    bench-parallel-virtual)  cmd_bench_parallel_virtual "${2:-50}" ;;
    bench-async-fanout)      cmd_bench_async_fanout "${2:-50}" ;;
    compare-parallel)        cmd_compare_parallel "${2:-50}" ;;
    compare-concurrency)     cmd_compare_concurrency "${2:-50}" ;;
    pinning-trace)           cmd_pinning_trace ;;
    all)                     cmd_all ;;
    ""|-h|--help)            usage ;;
    *)                       err "알 수 없는 명령: $1"; usage; exit 1 ;;
esac
