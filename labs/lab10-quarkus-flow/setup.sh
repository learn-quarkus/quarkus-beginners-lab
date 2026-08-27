#!/usr/bin/env bash
# Lab 10 setup script — brings barista-bot fully up to date for Lab 10.
#
# What this script does:
#   1. Copies the Lab 8 barista-bot solution into workshop/barista-bot/
#      (skips the copy if the directory already exists, applies updates in place)
#   2. Adds the quarkus-rest-client-jackson dependency to pom.xml
#   3. Writes OrderRequest.java, OrderResult.java, OrderFlowClient.java, OrderTools.java
#   4. Replaces BaristaAiService.java with the @ToolBox version
#   5. Appends order-flow-service REST client + updated MCP port to application.properties
#
# Run from the repo root:
#   bash labs/lab10-quarkus-flow/setup.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
SOURCE="$REPO_ROOT/labs/lab8-mcp-server/barista-bot"
DEST="$REPO_ROOT/workshop/barista-bot"
JAVA="$DEST/src/main/java/org/coffee"

# ── Step 1: Copy Lab 8 barista-bot ─────────────────────────────────────────────

if [ -d "$DEST" ]; then
  echo "ℹ️  '$DEST' already exists — skipping copy, applying updates in place."
else
  if [ ! -d "$SOURCE" ]; then
    echo "❌ Lab 8 barista-bot not found at $SOURCE"
    exit 1
  fi
  cp -r "$SOURCE" "$DEST"
  echo "✅ Copied Lab 8 barista-bot → $DEST"
fi

mkdir -p "$JAVA"

# ── Step 2: Add quarkus-rest-client-jackson to pom.xml ────────────────────────

POM="$DEST/pom.xml"

if grep -q "quarkus-rest-client-jackson" "$POM"; then
  echo "ℹ️  quarkus-rest-client-jackson already present in pom.xml — skipping."
else
  perl -i -0pe 's|(<artifactId>quarkus-langchain4j-mcp</artifactId>\s*</dependency>)|$1\n    <!-- REST client for Lab 10: calls order-flow-service -->\n    <dependency>\n      <groupId>io.quarkus</groupId>\n      <artifactId>quarkus-rest-client-jackson</artifactId>\n    </dependency>|' "$POM"
  echo "✅ Added quarkus-rest-client-jackson to pom.xml"
fi

# ── Step 3: Write new Java files ───────────────────────────────────────────────

cat > "$JAVA/OrderRequest.java" << 'EOF'
package org.coffee;

public class OrderRequest {
    public String itemName;
    public int    quantity;
    public String customerId;
    public double totalPrice;

    public OrderRequest() {}

    public OrderRequest(String itemName, int quantity, String customerId, double totalPrice) {
        this.itemName   = itemName;
        this.quantity   = quantity;
        this.customerId = customerId;
        this.totalPrice = totalPrice;
    }
}
EOF
echo "✅ Wrote OrderRequest.java"

cat > "$JAVA/OrderResult.java" << 'EOF'
package org.coffee;

public class OrderResult {
    public String orderId;
    public String status; // "CONFIRMED" or "PENDING_APPROVAL"

    public OrderResult() {}
}
EOF
echo "✅ Wrote OrderResult.java"

cat > "$JAVA/OrderFlowClient.java" << 'EOF'
package org.coffee;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "order-flow-service")
@Path("/flow")
public interface OrderFlowClient {

    @POST
    @Path("/order")
    OrderResult placeOrder(OrderRequest request);
}
EOF
echo "✅ Wrote OrderFlowClient.java"

cat > "$JAVA/OrderTools.java" << 'EOF'
package org.coffee;

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class OrderTools {

    @RestClient
    @Inject
    OrderFlowClient orderFlowClient;

    @Tool("Place a coffee order. Returns the order ID and status. "
        + "Use getItemPrice first to calculate totalPrice = price × quantity.")
    public String placeOrder(
            String item_name,
            int    quantity,
            String customer_id,
            double total_price) {

        OrderResult result = orderFlowClient.placeOrder(
            new OrderRequest(item_name, quantity, customer_id, total_price));

        if ("PENDING_APPROVAL".equals(result.status)) {
            return "Your order has been received but requires barista approval " +
                   "before it can be prepared (total $" + String.format("%.2f", total_price) +
                   " exceeds our express limit). Order ID: " + result.orderId +
                   ". A barista will confirm it shortly.";
        }
        return "Your order is confirmed! ☕ " + quantity + "× " + item_name +
               " — Order #" + result.orderId + ". Total: $" + String.format("%.2f", total_price);
    }
}
EOF
echo "✅ Wrote OrderTools.java"

# ── Step 4: Replace BaristaAiService.java with @ToolBox version ───────────────

cat > "$JAVA/BaristaAiService.java" << 'EOF'
package org.coffee;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
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
    When a customer asks to order, place, or buy an item:
      1. Use getItemPrice to look up the price per item.
      2. Calculate totalPrice = price × quantity.
      3. Use the placeOrder tool to submit the order.
    """)
public interface BaristaAiService {

    @McpToolBox("menu")
    @ToolBox(OrderTools.class)
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
EOF
echo "✅ Updated BaristaAiService.java with @ToolBox(OrderTools.class)"

# ── Step 5: Append config to application.properties ───────────────────────────

PROPS="$DEST/src/main/resources/application.properties"

if grep -q "order-flow-service" "$PROPS"; then
  echo "ℹ️  Lab 10 config already present in application.properties — skipping."
else
  # Repoint the inherited menu MCP URL to 8084 in place (order-service owns 8081)
  # rather than appending a second, contradictory copy of the key.
  if grep -q "quarkus.langchain4j.mcp.menu.url" "$PROPS"; then
    perl -i -pe 's|^(quarkus\.langchain4j\.mcp\.menu\.url=).*|${1}http://localhost:8084/mcp/sse|' "$PROPS"
  else
    printf '\nquarkus.langchain4j.mcp.menu.url=http://localhost:8084/mcp/sse\n' >> "$PROPS"
  fi
  cat >> "$PROPS" << 'EOF'

# ── Lab 10: REST client — connect to order-flow-service ───────────────────────
quarkus.rest-client.order-flow-service.url=http://localhost:8082
EOF
  echo "✅ Updated application.properties (MCP → 8084, added order-flow-service URL)"
fi

# ── Done ───────────────────────────────────────────────────────────────────────

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  barista-bot is ready for Lab 10                            ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "What was applied:"
echo "  ✅ Lab 8 barista-bot copied to:  $DEST"
echo "  ✅ pom.xml:                       added quarkus-rest-client-jackson"
echo "  ✅ New files:                     OrderRequest, OrderResult, OrderFlowClient, OrderTools"
echo "  ✅ BaristaAiService.java:         added @ToolBox(OrderTools.class)"
echo "  ✅ application.properties:        added order-flow-service URL + MCP port 8084"
echo ""
echo "Next steps — start all four services:"
echo ""
echo "  Terminal 1 — order-service (Lab 4):"
echo "    cd $REPO_ROOT/labs/lab4-kafka/solution/order-service && quarkus dev"
echo ""
echo "  Terminal 2 — menu-mcp-server on port 8084:"
echo "    cd $REPO_ROOT/labs/lab8-mcp-server/menu-mcp-server && quarkus dev -Dquarkus.http.port=8084"
echo ""
echo "  Terminal 3 — order-flow-service:"
echo "    cd \$YOUR_ORDER_FLOW_SERVICE_DIR && quarkus dev"
echo ""
echo "  Terminal 4 — barista-bot:"
echo "    cd $DEST"
echo "    export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-..."
echo "    quarkus dev"
echo ""
echo "  Then open http://localhost:8080 and say: I'd like 2 espressos please"
