#!/usr/bin/env bash
# Lab 8 setup script — copies the Lab 7 solution into a fresh barista-bot directory.
# Run from the repo root: bash labs/lab8-mcp-server/setup.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SOURCE="$REPO_ROOT/labs/lab7-langchain4j/solution"
DEST="$REPO_ROOT/barista-bot"

if [ -d "$DEST" ]; then
  echo "⚠️  '$DEST' already exists — skipping copy."
  echo "   Delete it first if you want a fresh start: rm -rf $DEST"
  exit 0
fi

if [ ! -d "$SOURCE" ]; then
  echo "❌ Lab 7 solution not found at $SOURCE"
  exit 1
fi

cp -r "$SOURCE" "$DEST"

echo ""
echo "✅ barista-bot is ready at: $DEST"
echo ""
echo "Next steps:"
echo "  cd barista-bot"
echo "  export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-..."
echo "  # Then follow the Lab 8 instructions in docs/lab8-mcp-server.md"
