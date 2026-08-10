#!/bin/bash
# =============================================================================
# Servlet AsyncContext POC - 테스트 러너
# 사용법:
#   ./run.sh start                  # 앱 백그라운드 시작
#   ./run.sh stop                   # 앱 중지
#   ./run.sh status                 # 앱 상태 확인
#   ./run.sh single                 # 단일 요청 (정상 점검)
#   ./run.sh compare [N]            # N 동시 blocking vs async 비교 (기본 50)
#   ./run.sh blocking [N]           # blocking만 N 동시
#   ./run.sh async [N]              # async만 N 동시
#   ./run.sh distribution [N]       # N 동시 응답시간 분포
#   ./run.sh all                    # 전체 시나리오 실행
# =============================================================================

set -euo pipefail

# ==================== 설정 ====================
PORT="${PORT:-8099}"
BASE="http://localhost:${PORT}"
APP_JAR="build/libs/app-async-context.jar"
APP_LOG="/tmp/servlet-async-poc.log"
APP_PID_FILE="/tmp/servlet-async-poc.pid"
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
    local max=30
    for i in $(seq 1 $max); do
        if curl -s -o /dev/null -m 1 "$BASE/blocking" 2>/dev/null; then
            return 0
        fi
        sleep 1
        printf "."
    done
    echo
    return 1
}

require_app() {
    if ! curl -s -o /dev/null -m 2 "$BASE/blocking" 2>/dev/null; then
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
        log "JAR 빌드 중... (:app-async-context:bootJar)"
        ./gradlew :app-async-context:bootJar -q
    fi

    log "앱 기동 (포트=$PORT)"
    nohup java -jar "$PROJECT_ROOT/$APP_JAR" --server.port="$PORT" > "$APP_LOG" 2>&1 &
    echo $! > "$APP_PID_FILE"
    local pid=$(cat $APP_PID_FILE)

    printf "대기 중"
    if ! wait_for_app; then
        err "앱 시작 실패. 로그 확인: $APP_LOG"
        tail -20 "$APP_LOG"
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
        if curl -s -o /dev/null -m 2 "$BASE/blocking"; then
            ok "HTTP 응답 정상"
        fi
    else
        err "중지됨"
    fi
}

# ==================== 테스트 함수 ====================
# 인자: 엔드포인트("/blocking" 또는 "/async"), 동시성, 총 요청 수
# 출력: wall_time_min avg_max throughput 반환 (전역 변수)
run_load() {
    local endpoint="$1"
    local concurrency="$2"
    local total="$3"
    local label="$4"

    local url="$BASE$endpoint"
    local tmpfile
    tmpfile=$(mktemp)

    # 각 요청의 time_total을 파일에 기록
    seq 1 "$total" | xargs -I{} -P "$concurrency" \
        curl -s -o /dev/null -w "%{time_total}\n" "$url" > "$tmpfile" 2>/dev/null

    # 통계 계산
    awk -v label="$label" '
        BEGIN { min=999999; max=0; sum=0; count=0 }
        { sum+=$1; count++; if($1<min)min=$1; if($1>max)max=$1 }
        END {
            avg = sum/count
            wall = max              # 동시 실행 시 진짜 wall 시간 = 가장 느린 요청
            rps = count/wall        # throughput = count / wall
            printf "LABEL=%s\nCOUNT=%d\nMIN=%.3f\nAVG=%.3f\nMAX=%.3f\nWALL=%.3f\nRPS=%.1f\n",
                   label, count, min, avg, max, wall, rps
        }
    ' "$tmpfile"
    rm -f "$tmpfile"
}

# run_load 결과 파싱하여 전역 변수에 저장
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

# ==================== 명령어 ====================
cmd_single() {
    require_app
    header "단일 요청 (정상 점검)"

    echo -e "${BOLD}/blocking${NC} 호출 중..."
    local b_time=$(curl -s -o /dev/null -w "%{time_total}" "$BASE/blocking")
    local b_body=$(curl -s "$BASE/blocking")
    echo -e "  응답 시간: ${YELLOW}${b_time}s${NC}"
    echo -e "  응답 본문: ${CYAN}${b_body}${NC}"

    echo
    echo -e "${BOLD}/async${NC} 호출 중..."
    local a_time=$(curl -s -o /dev/null -w "%{time_total}" "$BASE/async")
    local a_body=$(curl -s "$BASE/async")
    echo -e "  응답 시간: ${YELLOW}${a_time}s${NC}"
    echo -e "  응답 본문: ${CYAN}${a_body}${NC}"

    echo
    ok "둘 다 약 1초면 정상. 차이는 동시성에서 발생."
}

cmd_blocking() {
    require_app
    local n="${1:-50}"
    header "BLOCKING / 동시 $n"
    log "실행 중... (완료까지 기다리세요)"

    local result
    result=$(run_load "/blocking" "$n" "$n" "BLOCKING")
    parse_result "$result"

    print_summary
}

cmd_async() {
    require_app
    local n="${1:-50}"
    header "ASYNC / 동시 $n"
    log "실행 중..."

    local result
    result=$(run_load "/async" "$n" "$n" "ASYNC")
    parse_result "$result"

    print_summary
}

cmd_compare() {
    require_app
    local n="${1:-50}"
    header "비교 / 동시 $n 요청"

    echo -e "${BOLD}1) BLOCKING${NC} 실행 중..."
    local b_result
    b_result=$(run_load "/blocking" "$n" "$n" "BLOCKING")
    parse_result "$b_result"
    local b_wall=$RES_WALL b_avg=$RES_AVG b_max=$RES_MAX b_rps=$RES_RPS b_min=$RES_MIN

    echo -e "${BOLD}2) ASYNC${NC} 실행 중..."
    local a_result
    a_result=$(run_load "/async" "$n" "$n" "ASYNC")
    parse_result "$a_result"
    local a_wall=$RES_WALL a_avg=$RES_AVG a_max=$RES_MAX a_rps=$RES_RPS a_min=$RES_MIN

    # 비교 테이블
    echo
    echo -e "${BOLD}─────────── 결과 ───────────${NC}"
    printf "%-15s %-15s %-15s\n" "지표" "BLOCKING" "ASYNC"
    printf "%-15s %-15s %-15s\n" "───────────" "───────────" "───────────"
    printf "%-15s %-15s %-15s\n" "동시 요청"   "$n"               "$n"
    printf "%-15s %-15s %-15s\n" "Wall 시간"   "${b_wall}s"       "${a_wall}s"
    printf "%-15s %-15s %-15s\n" "최소 지연"   "${b_min}s"        "${a_min}s"
    printf "%-15s %-15s %-15s\n" "평균 지연"   "${b_avg}s"        "${a_avg}s"
    printf "%-15s %-15s %-15s\n" "최대 지연"   "${b_max}s"        "${a_max}s"
    printf "%-15s %-15s %-15s\n" "Throughput"  "${b_rps} rps"     "${a_rps} rps"

    # 개선율
    echo
    echo -e "${BOLD}─────────── 개선 ───────────${NC}"
    awk -v b="$b_wall" -v a="$a_wall" -v bmax="$b_max" -v amax="$a_max" -v brps="$b_rps" -v arps="$a_rps" '
        BEGIN {
            printf "Wall 시간 개선:   %.1fx 빠름\n", b/a
            printf "최대 지연 개선:   %.1fx 단축\n", bmax/amax
            printf "Throughput 증가:  %.1fx 증가\n", arps/brps
        }
    '
    echo
}

cmd_distribution() {
    require_app
    local n="${1:-100}"

    for ep in /blocking /async; do
        header "응답 시간 분포 / $ep / 동시 $n"
        log "실행 중..."

        local tmpfile
        tmpfile=$(mktemp)
        seq 1 "$n" | xargs -I{} -P "$n" \
            curl -s -o /dev/null -w "%{time_total}\n" "$BASE$ep" > "$tmpfile" 2>/dev/null

        # 정렬해서 보여줌 (히스토그램 대용)
        echo -e "${BOLD}모든 응답 시간 (초, 정렬됨):${NC}"
        sort -n "$tmpfile" | awk '
            BEGIN { prev=""; count=0 }
            {
                cur = sprintf("%.1f", $1)
                if (cur != prev) {
                    if (count > 0) printf "  %ss × %d\n", prev, count
                    prev = cur; count = 1
                } else {
                    count++
                }
            }
            END {
                if (count > 0) printf "  %ss × %d\n", prev, count
            }
        '
        rm -f "$tmpfile"
        echo
    done
}

cmd_all() {
    require_app
    header "전체 시나리오 순차 실행"

    log "1/4 단일 요청"
    cmd_single

    log "2/4 동시 50 비교"
    cmd_compare 50

    log "3/4 동시 100 비교"
    cmd_compare 100

    log "4/4 응답 시간 분포"
    cmd_distribution 100
}

print_summary() {
    echo
    echo -e "${BOLD}─────────── 결과 ───────────${NC}"
    printf "%-15s %s\n" "패턴"        "$RES_LABEL"
    printf "%-15s %d\n" "요청 수"      "$RES_COUNT"
    printf "%-15s %ss\n" "Wall 시간"   "$RES_WALL"
    printf "%-15s %ss\n" "최소 지연"   "$RES_MIN"
    printf "%-15s %ss\n" "평균 지연"   "$RES_AVG"
    printf "%-15s %ss\n" "최대 지연"   "$RES_MAX"
    printf "%-15s %s rps\n" "Throughput" "$RES_RPS"
    echo
}

# ==================== 도움말 ====================
usage() {
    cat <<EOF
${BOLD}사용법:${NC} ./run.sh <명령> [인자]

${BOLD}앱 제어:${NC}
  start                 앱 백그라운드 시작 (빌드 포함)
  stop                  앱 중지
  status                앱 실행 상태 확인

${BOLD}테스트:${NC}
  single                단일 요청 /blocking, /async 각각 (정상 점검용)
  blocking [N]          /blocking N 동시 요청 (기본 50)
  async    [N]          /async N 동시 요청 (기본 50)
  compare  [N]          blocking vs async side-by-side (기본 50)
  distribution [N]      N 동시 응답시간 분포 (기본 100)
  all                   전체 시나리오 순차 실행

${BOLD}환경 변수:${NC}
  PORT=8099             앱 포트 (기본 8099)

${BOLD}예시:${NC}
  ./run.sh start
  ./run.sh compare 100
  ./run.sh distribution 200
  PORT=9000 ./run.sh start

EOF
}

# ==================== 진입점 ====================
case "${1:-}" in
    start)         cmd_start ;;
    stop)          cmd_stop ;;
    status)        cmd_status ;;
    single)        cmd_single ;;
    blocking)      cmd_blocking "${2:-50}" ;;
    async)         cmd_async "${2:-50}" ;;
    compare)       cmd_compare "${2:-50}" ;;
    distribution)  cmd_distribution "${2:-100}" ;;
    all)           cmd_all ;;
    ""|-h|--help)  usage ;;
    *)             err "알 수 없는 명령: $1"; usage; exit 1 ;;
esac
