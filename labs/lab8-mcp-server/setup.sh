#!/usr/bin/env bash
# Lab 8 setup script — brings barista-bot fully up to date for Lab 8.
#
# What this script does:
#   1. Copies the Lab 7 solution into a fresh barista-bot/ directory (skips if already present)
#   2. Adds the quarkus-langchain4j-mcp dependency to pom.xml
#   3. Appends the MCP client config to application.properties
#   4. Replaces BaristaAiService.java with the @McpToolBox version
#
# Run from the repo root:
#   bash labs/lab8-mcp-server/setup.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SOURCE="$REPO_ROOT/labs/lab7-langchain4j/solution"
DEST="$REPO_ROOT/barista-bot"

# ── Step 1: Copy Lab 7 solution ────────────────────────────────────────────────

if [ -d "$DEST" ]; then
  echo "ℹ️  '$DEST' already exists — skipping copy, applying updates in place."
else
  if [ ! -d "$SOURCE" ]; then
    echo "❌ Lab 7 solution not found at $SOURCE"
    exit 1
  fi
  cp -r "$SOURCE" "$DEST"
  echo "✅ Copied Lab 7 solution → $DEST"
fi

# ── Step 2: Add quarkus-langchain4j-mcp to pom.xml ────────────────────────────

POM="$DEST/pom.xml"

if grep -q "quarkus-langchain4j-mcp" "$POM"; then
  echo "ℹ️  quarkus-langchain4j-mcp already present in pom.xml — skipping."
else
  # Insert after the quarkus-langchain4j-openai dependency block
  perl -i -0pe 's|(<artifactId>quarkus-langchain4j-openai</artifactId>\s*</dependency>)|$1\n    <!-- MCP client for Lab 8 -->\n    <dependency>\n      <groupId>io.quarkiverse.langchain4j</groupId>\n      <artifactId>quarkus-langchain4j-mcp</artifactId>\n    </dependency>|' "$POM"
  echo "✅ Added quarkus-langchain4j-mcp to pom.xml"
fi

# ── Step 3: Append MCP config to application.properties ───────────────────────

PROPS="$DEST/src/main/resources/application.properties"

if grep -q "quarkus.langchain4j.mcp" "$PROPS"; then
  echo "ℹ️  MCP config already present in application.properties — skipping."
else
  cat >> "$PROPS" << 'EOF'

# ── Lab 8: MCP client — connect to menu-mcp-server ────────────────────────────
quarkus.langchain4j.mcp.menu.transport-type=http
quarkus.langchain4j.mcp.menu.url=http://localhost:8081/mcp/sse
quarkus.langchain4j.mcp.menu.log-requests=true
quarkus.langchain4j.mcp.menu.log-responses=true
EOF
  echo "✅ Appended MCP config to application.properties"
fi

# ── Step 4: Replace BaristaAiService.java with @McpToolBox version ────────────

AI_SERVICE="$DEST/src/main/java/org/coffee/BaristaAiService.java"

cat > "$AI_SERVICE" << 'EOF'
package org.coffee;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
@SystemMessage("""
    You are a friendly and knowledgeable barista at The Quarkus Cafe.
    Answer questions about coffee, our menu, and brewing methods.
    Keep responses concise — 2-3 sentences maximum.
    If asked about something unrelated to coffee, politely redirect the conversation.
    When answering menu questions, use the available tools to get accurate, up-to-date information.
    """)
public interface BaristaAiService {

    @McpToolBox("menu")
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
EOF
echo "✅ Updated BaristaAiService.java with @McpToolBox"

# ── Done ───────────────────────────────────────────────────────────────────────

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  barista-bot is ready for Lab 8                             ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "What was applied:"
echo "  ✅ Lab 7 solution copied to:  $DEST"
echo "  ✅ pom.xml:                   added quarkus-langchain4j-mcp"
echo "  ✅ application.properties:    added MCP client config"
echo "  ✅ BaristaAiService.java:     added @McpToolBox(\"menu\")"
echo ""
echo "Next steps:"
echo ""
echo "  Terminal 1 — start the MCP server:"
echo "    cd $REPO_ROOT/labs/lab8-mcp-server/menu-mcp-server"
echo "    quarkus dev"
echo ""
echo "  Terminal 2 — start barista-bot:"
echo "    cd $DEST"
echo "    export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-..."
echo "    quarkus dev"
echo ""
echo "  Then open http://localhost:8080 and ask: What's on the menu?"
