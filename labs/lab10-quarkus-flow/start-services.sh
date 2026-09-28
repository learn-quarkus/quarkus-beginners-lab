#!/usr/bin/env bash
# Lab 10 — start all dependent services in one shot.
#
# Services started (each in its own background process):
#
#   Port 8081  order-service     (Lab 4 — stores orders, emits Kafka events)
#   Port 8082  order-flow-service (Lab 10 — Quarkus Flow HITL orchestrator)
#   Port 8084  menu-mcp-server   (Lab 8 — MCP tool server for menu prices)
#   Port 8080  barista-bot       (workshop/barista-bot — chat UI)
#
# For the first three, the copy under workshop/ is used when it exists, so you
# run the code you wrote. The labs/ reference copy is only a fallback.
#
# Prerequisites:
#   • Run `bash labs/lab10-quarkus-flow/setup.sh` first (sets up workshop/barista-bot)
#   • Set QUARKUS_LANGCHAIN4J_OPENAI_API_KEY in your environment (or export it before running)
#
# Usage (from repo root):
#   bash labs/lab10-quarkus-flow/start-services.sh
#
# All logs are tailed to the terminal. Press Ctrl-C once to stop everything.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# ── Service directories ────────────────────────────────────────────────────────
# Prefer the project you built yourself under workshop/, and fall back to the
# shipped reference copy only if that directory doesn't exist. Without this the
# script would always run the solutions, so a mistake in your own code would be
# invisible — the demo would work regardless of what you wrote.
pick_dir() {
  local mine="$1" fallback="$2"
  if [ -d "$mine" ]; then printf '%s' "$mine"; else printf '%s' "$fallback"; fi
}

ORDER_SERVICE_DIR="$(pick_dir "$REPO_ROOT/workshop/order-service" \
                              "$REPO_ROOT/labs/lab4-kafka/solution/order-service")"
ORDER_FLOW_DIR="$(pick_dir    "$REPO_ROOT/workshop/order-flow-service" \
                              "$REPO_ROOT/labs/lab10-quarkus-flow/solution/order-flow-service")"
MENU_MCP_DIR="$(pick_dir      "$REPO_ROOT/workshop/menu-mcp-server" \
                              "$REPO_ROOT/labs/lab8-mcp-server/menu-mcp-server")"
BARISTA_BOT_DIR="$REPO_ROOT/workshop/barista-bot"

# ── Ports ─────────────────────────────────────────────────────────────────────
PORT_ORDER_SERVICE=8081
PORT_ORDER_FLOW=8082
PORT_MENU_MCP=8084
PORT_BARISTA_BOT=8080

# ── Log files (written to labs/lab10-quarkus-flow/logs/) ──────────────────────
LOG_DIR="$SCRIPT_DIR/logs"
mkdir -p "$LOG_DIR"

LOG_ORDER_SERVICE="$LOG_DIR/order-service.log"
LOG_ORDER_FLOW="$LOG_DIR/order-flow-service.log"
LOG_MENU_MCP="$LOG_DIR/menu-mcp-server.log"
LOG_BARISTA_BOT="$LOG_DIR/barista-bot.log"

# ── PID tracking ──────────────────────────────────────────────────────────────
# PIDs of background service processes
PGIDS=()
# PID of the tail -F process
TAIL_PID=""

cleanup() {
  echo ""
  echo "⏹  Stopping all services…"
  # Stop the log tailer first
  if [ -n "$TAIL_PID" ] && kill -0 "$TAIL_PID" 2>/dev/null; then
    kill "$TAIL_PID" 2>/dev/null || true
  fi
  # Kill by port — catches the JVM child that quarkus dev forks
  for port in 8080 8081 8082 8084; do
    local_pid=$(lsof -t -i TCP:"$port" -sTCP:LISTEN 2>/dev/null || true)
    if [ -n "$local_pid" ]; then
      kill -9 $local_pid 2>/dev/null || true
    fi
  done
  # Also kill tracked PIDs and their children
  for pid in "${PGIDS[@]}"; do
    pkill -P "$pid" 2>/dev/null || true
    kill "$pid" 2>/dev/null || true
  done
  echo "✅ All services stopped."
  exit 0
}
trap cleanup INT TERM

# ── Helpers ───────────────────────────────────────────────────────────────────

check_dir() {
  local dir="$1" label="$2"
  if [ ! -d "$dir" ]; then
    echo "❌ $label not found at: $dir"
    exit 1
  fi
}

port_free() {
  ! lsof -i TCP:"$1" -sTCP:LISTEN -t &>/dev/null
}

wait_for_port() {
  local port="$1" label="$2" retries=60 i=0
  echo -n "   ⏳ Waiting for $label (port $port)"
  while ! lsof -i TCP:"$port" -sTCP:LISTEN -t &>/dev/null; do
    sleep 2
    i=$((i + 1))
    if [ $i -ge $retries ]; then
      echo ""
      echo "   ❌ $label did not start within $(( retries * 2 ))s — check $LOG_DIR"
      return 1
    fi
    echo -n "."
  done
  echo " ready ✅"
}

start_service() {
  local label="$1" dir="$2" port="$3" log="$4"
  shift 4                    # remaining args are the command to run

  echo ""
  echo "▶  Starting $label on port $port …"
  echo "   Log: $log"

  if ! port_free "$port"; then
    echo "   ⚠️  Port $port is already in use — assuming $label is already running, skipping."
    return
  fi

  (cd "$dir" && "$@") > "$log" 2>&1 &
  local pid=$!
  PGIDS+=("$pid")

  wait_for_port "$port" "$label"
}

# ── Pre-flight checks ─────────────────────────────────────────────────────────

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  Lab 10 — starting all dependent services                   ║"
echo "╚══════════════════════════════════════════════════════════════╝"

check_dir "$ORDER_SERVICE_DIR"  "order-service (Lab 4)"
check_dir "$ORDER_FLOW_DIR"     "order-flow-service (Lab 10)"
check_dir "$MENU_MCP_DIR"       "menu-mcp-server (Lab 8)"

# Auto-run setup.sh if workshop/barista-bot hasn't been created yet
if [ ! -d "$BARISTA_BOT_DIR" ]; then
  echo ""
  echo "ℹ️  workshop/barista-bot not found — running setup.sh first…"
  bash "$SCRIPT_DIR/setup.sh"
  echo ""
fi

check_dir "$BARISTA_BOT_DIR"    "barista-bot (workshop)"

# Show which copy of each project is actually being run.
echo ""
echo "  Using these project directories:"
for entry in \
    "order-service|$ORDER_SERVICE_DIR" \
    "order-flow-service|$ORDER_FLOW_DIR" \
    "menu-mcp-server|$MENU_MCP_DIR" \
    "barista-bot|$BARISTA_BOT_DIR"; do
  name="${entry%%|*}"
  dir="${entry#*|}"
  case "$dir" in
    "$REPO_ROOT"/workshop/*) origin="yours" ;;
    *)                       origin="reference copy — you have no workshop/$name" ;;
  esac
  printf '    %-19s %s  (%s)\n' "$name" "${dir#$REPO_ROOT/}" "$origin"
done
echo ""

if [ -z "${QUARKUS_LANGCHAIN4J_OPENAI_API_KEY:-}" ]; then
  echo ""
  echo "⚠️  QUARKUS_LANGCHAIN4J_OPENAI_API_KEY is not set."
  echo "   barista-bot will start but the LLM will refuse requests."
  echo "   Set it with:  export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-..."
  echo ""
fi

# ── Start services (in dependency order) ──────────────────────────────────────

# 1. order-service (port 8081) — order-flow-service depends on this
start_service \
  "order-service" \
  "$ORDER_SERVICE_DIR" \
  "$PORT_ORDER_SERVICE" \
  "$LOG_ORDER_SERVICE" \
  quarkus dev

# 2. menu-mcp-server (port 8084) — barista-bot depends on this
#    Its application.properties already sets 8084; passed again here so the
#    port is correct even for an older copy that still defaults to 8081.
start_service \
  "menu-mcp-server" \
  "$MENU_MCP_DIR" \
  "$PORT_MENU_MCP" \
  "$LOG_MENU_MCP" \
  quarkus dev -Dquarkus.http.port=8084

# 3. order-flow-service (port 8082) — barista-bot depends on this
start_service \
  "order-flow-service" \
  "$ORDER_FLOW_DIR" \
  "$PORT_ORDER_FLOW" \
  "$LOG_ORDER_FLOW" \
  quarkus dev

# 4. barista-bot (port 8080) — depends on all of the above
start_service \
  "barista-bot" \
  "$BARISTA_BOT_DIR" \
  "$PORT_BARISTA_BOT" \
  "$LOG_BARISTA_BOT" \
  quarkus dev

# ── All up ────────────────────────────────────────────────────────────────────

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  All services are up!                                       ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "  Service              Port   Log"
echo "  ───────────────────  ─────  ──────────────────────────────────"
echo "  order-service        8081   logs/order-service.log"
echo "  order-flow-service   8082   logs/order-flow-service.log"
echo "  menu-mcp-server      8084   logs/menu-mcp-server.log"
echo "  barista-bot          8080   logs/barista-bot.log"
echo ""
echo "  Chat UI   → http://localhost:8080"
echo "  Admin UI  → http://localhost:8082/admin"
echo "  Flow UI   → http://localhost:8082/q/dev-ui"
echo ""
echo "Press Ctrl-C to stop all services."
echo ""

# ── Tail all logs to stdout so the operator can see everything ─────────────────
tail -F \
  "$LOG_ORDER_SERVICE" \
  "$LOG_ORDER_FLOW" \
  "$LOG_MENU_MCP" \
  "$LOG_BARISTA_BOT" &
TAIL_PID=$!

# Keep the script alive so the trap fires on Ctrl-C
wait
