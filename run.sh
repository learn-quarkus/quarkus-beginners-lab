#!/bin/bash
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"

cd "$DIR/workshop-runner"
npm install --silent 2>/dev/null

echo "Starting workshop runner + MkDocs..."
echo ""

# Start the command execution server in the background
node server.js "$DIR/workshop" &
WS_PID=$!

# Start MkDocs
cd "$DIR"
mkdocs serve &
MKDOCS_PID=$!

echo ""
echo "  MkDocs:          http://localhost:8000"
echo "  Workshop Runner:  ws://localhost:3001"
echo ""
echo "Press Ctrl+C to stop both."

trap "kill $WS_PID $MKDOCS_PID 2>/dev/null" EXIT
wait
