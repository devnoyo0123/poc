#!/usr/bin/env bash
# End-to-end scenario runner: real MySQL + real HTTP (WireMock) + curl.
# Mirrors the "SQL seed -> call API -> verify response/DB" pattern.
#
#   ./scenario/run-scenario.sh
#
# Steps: build jar -> docker up (mysql+wiremock) -> seed SQL -> start app
#        (scenario profile) -> curl scenarios -> assert -> teardown.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

COMPOSE="docker compose -f scenario/docker-compose.test.yml"
APP_PORT="8082"
APP_URL="http://localhost:${APP_PORT}"
JAR="jpa-servlet/build/libs/jpa-servlet-0.0.1-SNAPSHOT.jar"
APP_PID=""
PASS=0; FAIL=0

log()  { echo -e "\033[1;36m[scenario]\033[0m $*"; }
ok()   { echo -e "  \033[1;32m✅ $*\033[0m"; PASS=$((PASS+1)); }
bad()  { echo -e "  \033[1;31m❌ $*\033[0m"; FAIL=$((FAIL+1)); }

cleanup() {
  log "cleanup"
  [ -n "$APP_PID" ] && kill "$APP_PID" 2>/dev/null || true
  $COMPOSE down -v >/dev/null 2>&1 || true
}
trap cleanup EXIT

# DB query helper (direct DB check, like the skill does)
db() { $COMPOSE exec -T mysql mysql -N -s -utxuser -ptxpass txdemo -e "$1" 2>/dev/null; }

# --- 1. build -------------------------------------------------------------
log "building app jar"
./gradlew :jpa-servlet:bootJar -q

# --- 2. containers --------------------------------------------------------
log "starting MySQL + WireMock"
$COMPOSE up -d

log "waiting for MySQL healthy"
for i in $(seq 1 40); do
  status=$($COMPOSE ps --format '{{.Health}}' mysql 2>/dev/null || true)
  [ "$status" = "healthy" ] && break
  sleep 2
done
[ "$status" = "healthy" ] || { bad "MySQL not healthy"; exit 1; }

# --- 3. seed (creates schema + 2 rows; ddl-auto=none) ---------------------
log "seeding SQL data"
$COMPOSE exec -T mysql mysql -utxuser -ptxpass txdemo < scenario/data/seed.sql
seed_count=$(db "SELECT COUNT(*) FROM work_log;")
[ "$seed_count" = "2" ] && ok "seed: 2 pre-existing rows" || bad "seed count=$seed_count (want 2)"

# --- 4. start app (scenario profile) --------------------------------------
log "starting app (profile=scenario)"
java -jar "$JAR" --spring.profiles.active=scenario --server.port=$APP_PORT >/tmp/tx-scenario-app.log 2>&1 &
APP_PID=$!

log "waiting for app"
for i in $(seq 1 40); do
  curl -fsS "$APP_URL/logs" >/dev/null 2>&1 && break
  sleep 1
done
curl -fsS "$APP_URL/logs" >/dev/null 2>&1 || { bad "app did not start (see /tmp/tx-scenario-app.log)"; exit 1; }

# --- 5. scenarios ---------------------------------------------------------
log "STEP 1: fixed + external 500 -> FAILED committed in new tx"
curl -fsS -XPOST "$APP_URL/fixed?fail=true" >/dev/null
failed_cnt=$(db "SELECT COUNT(*) FROM work_log WHERE status='FAILED';")
[ "$failed_cnt" = "1" ] && ok "1 FAILED row persisted" || bad "FAILED count=$failed_cnt (want 1)"

log "STEP 2: broken + external 500 -> rollback-only, nothing new"
before=$(db "SELECT COUNT(*) FROM work_log;")
curl -fsS -XPOST "$APP_URL/broken?fail=true" >/dev/null || true
after=$(db "SELECT COUNT(*) FROM work_log;")
[ "$before" = "$after" ] && ok "no new row from broken (before=$before after=$after)" \
  || bad "broken changed row count ($before -> $after)"

log "STEP 3: fixed + external 200 -> SUCCESS committed"
curl -fsS -XPOST "$APP_URL/fixed?fail=false" >/dev/null
success_cnt=$(db "SELECT COUNT(*) FROM work_log WHERE status='SUCCESS' AND detail='api ok';")
[ "$success_cnt" = "1" ] && ok "1 SUCCESS row persisted" || bad "SUCCESS(api ok) count=$success_cnt (want 1)"

# --- 6. report ------------------------------------------------------------
echo
log "final DB state:"
db "SELECT id,status,detail FROM work_log ORDER BY id;" | sed 's/^/    /'
echo
log "result: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
