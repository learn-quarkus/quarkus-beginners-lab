#!/usr/bin/env bash
# Lab 9 setup script — copies the Lab 3 solution into menu-service/
#
# Use this if you don't have your own menu-service from Lab 3.
#
# Run from the repo root:
#   bash labs/lab9-containerize/setup.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SOURCE="$REPO_ROOT/labs/lab3-config-health/solution"
DEST="$REPO_ROOT/menu-service"

# ── Copy Lab 3 solution ────────────────────────────────────────────────────────

if [ -d "$DEST" ]; then
  echo "ℹ️  '$DEST' already exists — nothing to do."
  echo "    If you want a clean copy, remove it first: rm -rf $DEST"
  exit 0
fi

if [ ! -d "$SOURCE" ]; then
  echo "❌ Lab 3 solution not found at $SOURCE"
  exit 1
fi

cp -r "$SOURCE" "$DEST"
echo "✅ Copied Lab 3 solution → $DEST"

# ── Add the Dockerfile (not present in Lab 3 solution) ────────────────────────

mkdir -p "$DEST/src/main/docker"
cp "$SCRIPT_DIR/solution/src/main/docker/Dockerfile.jvm" \
   "$DEST/src/main/docker/Dockerfile.jvm"
echo "✅ Added src/main/docker/Dockerfile.jvm"

# ── Done ───────────────────────────────────────────────────────────────────────

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  menu-service is ready for Lab 9                            ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "What was applied:"
echo "  ✅ Lab 3 solution copied to:  $DEST"
echo "  ✅ Dockerfile.jvm added to:   $DEST/src/main/docker/"
echo ""
echo "Next steps:"
echo ""
echo "  1. Build the JAR:"
echo "       cd $DEST"
echo "       mvn package"
echo ""
echo "  2. Build the container image:"
echo "       podman build -f src/main/docker/Dockerfile.jvm -t menu-service:1.0 ."
echo ""
echo "  3. Run it:"
echo "       podman run --rm -p 8080:8080 menu-service:1.0"
echo ""
echo "  4. Test it:"
echo "       curl http://localhost:8080/menu"
