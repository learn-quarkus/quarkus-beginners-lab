#!/usr/bin/env bash
# Lab 9 setup script — copies the Lab 9 solution into a fresh lab9-menu-service/ directory.
#
# Run from the repo root:
#   bash labs/lab9-containerize/setup.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SOURCE="$SCRIPT_DIR/solution"
DEST="$REPO_ROOT/workshop/lab9-menu-service"

# ── Copy Lab 9 solution ────────────────────────────────────────────────────────

if [ -d "$DEST" ]; then
  echo "ℹ️  '$DEST' already exists — nothing to do."
  echo "    If you want a clean copy, remove it first: rm -rf $DEST"
  exit 0
fi

if [ ! -d "$SOURCE" ]; then
  echo "❌ Lab 9 solution not found at $SOURCE"
  exit 1
fi

cp -r "$SOURCE" "$DEST"

# ── Done ───────────────────────────────────────────────────────────────────────

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  lab9-menu-service is ready — continue with the lab steps   ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "  cd $DEST"
