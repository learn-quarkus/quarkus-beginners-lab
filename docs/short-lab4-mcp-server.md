# Lab 4: MCP Server — Expose Your Menu as an AI Tool

**Duration:** 10 minutes &nbsp;|&nbsp; **Projects:** `menu-mcp-server` (new) + `barista-bot` (from Lab 3)

!!! info "What you'll build"
    - **`menu-mcp-server`** — a tiny Quarkus app on port `8084` that exposes menu data as **MCP tools** (`getMenuItems`, `getItemPrice`, `getItemsByMilkOption`)
    - **`barista-bot`** — updated to call those tools via the **MCP client** extension; the LLM automatically invokes the right tool when answering menu questions

    **Result:** Ask the barista bot *"What can I get with oat milk under $4?"* and it calls `getMenuItems()` over MCP, gets live structured data, and answers from it — no vector store, no embedding model, no similarity search.

!!! success "The Solution: Grounding in Live Data"
    Instead of relying on training data (Lab 7's problem), the bot now calls **live backend methods** for exact, current information. No hallucination — just facts from your actual system.


!!! note "MCP in one sentence"
    **Model Context Protocol (MCP)** is an open standard for exposing tools, data, and prompts to AI assistants. Your Quarkus service becomes a tool server any LLM can call.

**Reference:** [Quarkus MCP Server docs](https://docs.quarkiverse.io/quarkus-mcp-server/dev/){ target="_blank" }

---

!!! tip "Working directory"
    All commands in this lab run from the `workshop/` folder inside the cloned repo. Make sure you are in that folder before you begin.

---

## Step 1 — Create `menu-mcp-server`

In a **new terminal**, create the MCP server project:

=== "Quarkus CLI"

    ```bash
    quarkus create app org.coffee:menu-mcp-server \
      --extensions=mcp-server-sse
    cd menu-mcp-server
    ```

=== "Maven"

    ```bash
    mvn io.quarkus.platform:quarkus-maven-plugin:3.39.2:create \
      -DprojectGroupId=org.coffee \
      -DprojectArtifactId=menu-mcp-server \
      -Dextensions=mcp-server-sse
    cd menu-mcp-server
    ```

!!! note "No sample files to delete here"
    Unlike the other labs, the `mcp-server-sse` extension has no REST codestart, so this
    project is generated **without** any `GreetingResource` class or tests. `src/main/java/org/coffee/`
    is empty and there is nothing to clean up — go straight to Step 2.

---

## Step 2 — Define the MCP tools

Create `src/main/java/org/coffee/MenuTools.java`:

```bash
mkdir -p src/main/java/org/coffee && touch src/main/java/org/coffee/MenuTools.java
```

Open `MenuTools.java` in your IDE and paste in the following:

```java
package org.coffee;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class MenuTools {

    // The full menu — in a real app this would come from a database
    private static final List<Map<String, Object>> MENU = List.of(
        Map.of("name", "Espresso",    "price", 2.50, "milk", List.of()),
        Map.of("name", "Cappuccino",  "price", 3.75, "milk", List.of("whole", "oat")),
        Map.of("name", "Flat White",  "price", 4.00, "milk", List.of("whole")),
        Map.of("name", "Latte",       "price", 4.25, "milk", List.of("whole", "oat", "almond", "soy")),
        Map.of("name", "Americano",   "price", 3.00, "milk", List.of()),
        Map.of("name", "Cold Brew",   "price", 4.00, "milk", List.of("whole", "oat")),
        Map.of("name", "Iced Latte",  "price", 4.50, "milk", List.of("whole", "oat", "almond"))
    );

    @Tool(description = "Get all menu items with their name, price, and available milk options") // 
    public String getMenuItems() {
        return MENU.stream()
            .map(item -> String.format("%-12s $%.2f  milk: %s",
                item.get("name"), item.get("price"),
                ((List<?>) item.get("milk")).isEmpty() ? "none" : item.get("milk")))
            .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Get the price of a specific menu item by name")
    public String getItemPrice(
            @ToolArg(description = "Name of the menu item, e.g. Espresso") String name) { // 
        return MENU.stream()
            .filter(item -> item.get("name").toString().equalsIgnoreCase(name))
            .map(item -> String.format("%s costs $%.2f", item.get("name"), item.get("price")))
            .findFirst()
            .orElse("Item '" + name + "' not found on the menu.");
    }

    @Tool(description = "Get menu items available with a specific milk option")
    public String getItemsByMilkOption(
            @ToolArg(description = "Milk type: whole, oat, almond, or soy") String milk) {
        var matches = MENU.stream()
            .filter(item -> ((List<?>) item.get("milk")).stream()
                .anyMatch(m -> m.toString().equalsIgnoreCase(milk)))
            .map(item -> String.format("%s ($%.2f)", item.get("name"), item.get("price")))
            .collect(Collectors.joining(", "));
        return matches.isEmpty()
            ? "No items available with " + milk + " milk."
            : "Items with " + milk + " milk: " + matches;
    }

    @Tool(description = "Place an order and send it to the order service")
    public String placeOrder(
            @ToolArg(description = "Item name to order, e.g. Espresso") String itemName,
            @ToolArg(description = "Quantity to order") int quantity) {
        System.out.println("Placing order for " + quantity + " x " + itemName);
        
                if (itemName == null || itemName.isBlank()) {
            return "Please specify an item to order.";
        }

        // Validate item exists
        var item = MENU.stream()
                .filter(m -> m.get("name").toString().equalsIgnoreCase(itemName.trim()))
                .findFirst();
        if (item.isEmpty()) {
            return "Item '" + itemName + "' not found on the menu.";
        }

        // Send order to order-service on port 8081
        try {
            var client = java.net.http.HttpClient.newHttpClient();
            var jsonBody = String.format("{\"itemName\":\"%s\",\"quantity\":%d,\"customerId\":\"asilva\"}",
                    itemName, quantity);
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(new java.net.URI("http://localhost:8081/orders"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300
                    ? String.format("Order placed! ✓ %d x %s", quantity, itemName)
                    : "Order failed: " + response.statusCode();
        } catch (Exception e) {
            return "Error placing order: " + e.getMessage();
        }
    }
}
```

1. `@Tool` — registers this method as an MCP tool. The `description` is sent to the LLM so it knows when to call it.
2. `@ToolArg` — documents the parameter. Quarkus generates the JSON schema automatically.

!!! note "What just happened?"
    Quarkus scans `@Tool` methods at build time, generates JSON schema for each parameter, and exposes them all over MCP — at `/mcp` (streamable HTTP) and `/mcp/sse` (legacy SSE). No manual registration, no routing code.

---

## Step 3 — Configure the server port

Open `src/main/resources/application.properties` and set the port so it doesn't clash with the other services:

```properties title="application.properties"
quarkus.http.port=8084
```

!!! warning "Why 8084 and not 8081?"
    Quarkus uses **8081 as the default test port**. To avoid port conflicts, `menu-mcp-server` runs on 8084.

---

## Step 4 — Start the MCP server

```bash
quarkus dev
```

The server starts on `http://localhost:8084`. You can verify the tools are exposed by opening the Dev UI:

**`http://localhost:8084/q/dev-ui`** → find **MCP Server – HTTP/SSE** → click **Tools**

You should see `getMenuItems`, `getItemPrice`, `getItemsByMilkOption`, and `placeOrder` listed.

---

## Step 5 — Wire `barista-bot` as an MCP client

Back in your `barista-bot` directory, add the MCP client extension:

=== "Quarkus CLI"

    ```bash
    quarkus ext add quarkus-langchain4j-mcp
    ```

=== "Maven"

    ```bash
    mvn quarkus:add-extension -Dextensions="quarkus-langchain4j-mcp"
    ```

---

## Step 6 — Configure the MCP connection

Open `barista-bot/src/main/resources/application.properties` and add:

```properties
quarkus.langchain4j.mcp.menu.transport-type=streamable-http
quarkus.langchain4j.mcp.menu.url=http://localhost:8084/mcp
quarkus.langchain4j.mcp.menu.log-requests=true
quarkus.langchain4j.mcp.menu.log-responses=true
```

1. `streamable-http` is the HTTP transport for MCP. The connection name is `menu` (the string you pass to `@McpToolBox`). The only accepted values are `stdio`, `websocket` and `streamable-http` — anything else fails at startup with `SRCFG00049: Cannot convert ...`.
2. Point the client at **`/mcp`**, not `/mcp/sse`. The server exposes both endpoints — you'll see them logged on startup as `MCP HTTP transport endpoints [streamable: http://localhost:8084/mcp, SSE: http://localhost:8084/mcp/sse]` — but `/mcp/sse` is the legacy SSE endpoint and rejects the streamable client's POST with **HTTP 405**.

---

## Step 7 — Tell the AI service to use the tools

Open `BaristaAiService.java` and add `@McpToolBox`. The `chat` method now takes **two arguments** — `@MemoryId` and `@UserMessage` — to support per-session memory:

```java
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

    @McpToolBox("menu")                                              // (1)
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
```

1. `@McpToolBox("menu")` — wires the `menu` MCP client (configured in `application.properties`) to this method. The LLM automatically decides which tool to call based on the question.

!!! note "What just happened?"
    LangChain4j fetches the tool definitions from `menu-mcp-server` at startup, sends them to the LLM as part of every request, and executes any tool call the LLM requests — transparently, before returning the final answer.

---

## Step 8 — Start `barista-bot` and test

In a second terminal inside `barista-bot`:

=== "Quarkus CLI"

    ```bash
    quarkus dev
    ```

=== "Maven"

    ```bash
    mvn quarkus:dev
    ```

Open **`http://localhost:8082`** and try these questions:

| Question | Tool the LLM is expected to pick |
|----------|----------------------------------|
| `What's on the menu?` | `getMenuItems()` |
| `How much is a Flat White?` | `getItemPrice("Flat White")` |
| `What can I get with oat milk under $4?` | `getMenuItems()`, then filters in its reasoning |
| `Do you have almond milk options?` | `getItemsByMilkOption("almond")` |
| `Place order for 1 Americano` | `getItemsByMilkOption("almond")` |

Watch the `barista-bot` terminal — with `log-requests=true` you'll see the MCP tool calls logged as they happen.

!!! note "Tool selection is the model's decision, not a guarantee"
    Nothing forces the LLM to call a tool. It sees the tool definitions alongside the
    question and decides. So treat the table above as *what should happen*, not a contract —
    a given run may answer one of these from general knowledge instead.

    To confirm a tool call really happened, look for the JSON-RPC exchange in the
    `barista-bot` terminal rather than judging by the answer text:

    ```
    Request: {"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"getItemsByMilkOption",...
    ```

    If you see no `tools/call` at all, the usual causes are:

    1. **`menu-mcp-server` isn't running** on port 8084, so the tool list came back empty.
    2. The question was vague enough that the model felt it could answer without help — try
       `Which items can I get with almond milk?`, which is the most reliable trigger.

---

## Summary

| What | How |
|------|-----|
| ✅ MCP server in one class | `@Tool` + `@ToolArg` on any CDI bean |
| ✅ Zero routing boilerplate | Quarkus auto-exposes `/mcp` |
| ✅ AI uses live structured data | `@McpToolBox("menu")` on the AI service method |
| ✅ No extra API keys | `menu-mcp-server` is pure Java — no LLM calls |

!!! tip "Stuck or fell behind?"
    Complete solutions are in `labs/lab8-mcp-server/`:

    ```bash
    # Terminal 1 — MCP server:
    cd labs/lab8-mcp-server/menu-mcp-server && quarkus dev

    # Terminal 2 — barista-bot with MCP client:
    cd labs/lab8-mcp-server/barista-bot && quarkus dev
    ```

---

[← Lab 3: AI Chatbot](short-lab3-langchain4j.md){ .md-button }
[→ Done! Back to Quick Start](short-menu.md){ .md-button .md-button--primary }
