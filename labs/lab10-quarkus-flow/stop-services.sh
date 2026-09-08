#!/usr/bin/env bash
# Lab 10 — stop all dependent services.
#
# Kills any process listening on the four Lab 10 ports:
#   8080  barista-bot
#   8081  order-service
#   8082  order-flow-service
#   8084  menu-mcp-server
#
# Usage (from anywhere):
#   bash labs/lab10-quarkus-flow/stop-services.sh

PORTS=(8080 8081 8082 8084)
LABELS=(barista-bot order-service order-flow-service menu-mcp-server)

echo "⏹  Stopping Lab 10 services…"
echo ""

any=0
for i in "${!PORTS[@]}"; do
  port="${PORTS[$i]}"
  label="${LABELS[$i]}"
  pid=$(lsof -t -i TCP:"$port" -sTCP:LISTEN 2>/dev/null || true)
  if [ -n "$pid" ]; then
    kill -9 $pid 2>/dev/null || true
    echo "  ✅ $label (port $port) — stopped"
    any=1
  else
    echo "  ─  $label (port $port) — not running"
  fi
done

echo ""
[ $any -eq 1 ] && echo "Done." || echo "Nothing to stop."
