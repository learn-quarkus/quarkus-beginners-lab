# Lab 8: MCP Server — Expose Your Menu as an AI Tool

**Duration:** 10 minutes &nbsp;|&nbsp; **Projects:** `menu-mcp-server` (new) + `barista-bot` (from Lab 7)

!!! warning "Lab 7 required"
    This lab builds on `barista-bot` from Lab 7. If you didn't complete Lab 7, run the setup script below to get the starting point.

!!! info "What you'll build"
    - **`menu-mcp-server`** — a tiny Quarkus app on port `8081` that exposes menu data as **MCP tools** (`getMenuItems`, `getItemPrice`, `getItemsByMilkOption`)
    - **`barista-bot`** — updated to call those tools via the **MCP client** extension; the LLM automatically invokes the right tool when answering menu questions

    **Result:** Ask the barista bot *"What can I get with oat milk under $4?"* and it calls `getMenuItems()` over MCP, gets live structured data, and answers accurately — no RAG, no hallucination.

!!! note "MCP in one sentence"
    **Model Context Protocol (MCP)** is an open standard for exposing tools, data, and prompts to AI assistants. Your Quarkus service becomes a tool server any LLM can call.

**Reference:** [Quarkus MCP Server docs](https://docs.quarkiverse.io/quarkus-mcp-server/dev/){ target="_blank" }

---

## Setup — Get the starting point

If you completed Lab 7 your `barista-bot` directory is already ready. Skip to [Step 1](#step-1-create-menu-mcp-server).

If you didn't finish Lab 7, run the setup script from the repo root:

```bash
bash labs/lab8-mcp-server/setup.sh
```

The script does four things automatically:

1. Copies the Lab 7 solution into a fresh `barista-bot` directory
2. Adds the `quarkus-langchain4j-mcp` extension to `pom.xml`
3. Appends the MCP client configuration to `application.properties`
4. Replaces `BaristaAiService.java` with the `@McpToolBox`-enabled version

It is **idempotent** — safe to run again if something goes wrong.

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
    mvn io.quarkus.platform:quarkus-maven-plugin:3.33.3:create \
      -DprojectGroupId=org.coffee \
      -DprojectArtifactId=menu-mcp-server \
      -Dextensions=mcp-server-sse
    cd menu-mcp-server
    ```

!!! tip "Delete the generated sample files"
    ```bash
    rm src/main/java/org/coffee/GreetingResource.java
    rm src/test/java/org/coffee/GreetingResourceTest.java
    rm src/test/java/org/coffee/GreetingResourceIT.java
    ```

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

    @Tool(description = "Get all menu items with their name, price, and available milk options") // (1)
    public String getMenuItems() {
        return MENU.stream()
            .map(item -> String.format("%-12s $%.2f  milk: %s",
                item.get("name"), item.get("price"),
                ((List<?>) item.get("milk")).isEmpty() ? "none" : item.get("milk")))
            .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Get the price of a specific menu item by name")
    public String getItemPrice(
            @ToolArg(description = "Name of the menu item, e.g. Espresso") String name) { // (2)
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
}
```

1. `@Tool` — registers this method as an MCP tool. The `description` is sent to the LLM so it knows when to call it.
2. `@ToolArg` — documents the parameter. Quarkus generates the JSON schema automatically.

!!! note "What just happened?"
    Quarkus scans `@Tool` methods at build time, generates JSON schema for each parameter, and exposes them all at `/mcp/sse`. No manual registration, no routing code.

---

## Step 3 — Configure the server port

Open `src/main/resources/application.properties` and set the port so it doesn't clash with `barista-bot`:

```properties title="application.properties"
quarkus.http.port=8081
```

---

## Step 4 — Start the MCP server

```bash
quarkus dev
```

The server starts on `http://localhost:8081`. You can verify the tools are exposed by opening the Dev UI:

**`http://localhost:8081/q/dev-ui`** → find **MCP Server – HTTP/SSE** → click **Tools**

You should see `getMenuItems`, `getItemPrice`, and `getItemsByMilkOption` listed.

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
# MCP client — connect to menu-mcp-server on port 8081
# 1 (1)
quarkus.langchain4j.mcp.menu.transport-type=http
# 2 (2)
quarkus.langchain4j.mcp.menu.url=http://localhost:8081/mcp/sse
quarkus.langchain4j.mcp.menu.log-requests=true
quarkus.langchain4j.mcp.menu.log-responses=true
```

1. `transport-type=http` — HTTP/SSE transport; the connection name is `menu` (used in `@McpToolBox`).
2. The MCP server auto-exposes the SSE endpoint at `/mcp/sse`.

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

Open **`http://localhost:8080`** and try these questions:

| Question | What happens under the hood |
|----------|-----------------------------|
| `What's on the menu?` | LLM calls `getMenuItems()` |
| `How much is a Flat White?` | LLM calls `getItemPrice("Flat White")` |
| `What can I get with oat milk under $4?` | LLM calls `getMenuItems()`, filters in its reasoning |
| `Do you have almond milk options?` | LLM calls `getItemsByMilkOption("almond")` |

Watch the `barista-bot` terminal — with `log-requests=true` you'll see the MCP tool calls logged as they happen.

!!! note "MCP vs Easy RAG (from Lab 7)"
    | | Easy RAG | MCP tools |
    |--|----------|-----------|
    | Data source | Text file (`menu.txt`) | Live method call |
    | Accuracy | Fuzzy similarity search | Exact programmatic result |
    | Data updates | Requires app restart | Always fresh |
    | Setup | One property + one file | One annotation + one property |

    Both solve the hallucination problem — MCP tools give you **precision and freshness** at the cost of writing the tool methods.

---

## Summary

| What | How |
|------|-----|
| ✅ MCP server in one class | `@Tool` + `@ToolArg` on any CDI bean |
| ✅ Zero routing boilerplate | Quarkus auto-exposes `/mcp/sse` |
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

[← Lab 7: AI with LangChain4j](lab7-langchain4j.md){ .md-button }
[→ Lab 9: Containerize & K8s](lab9-containerize.md){ .md-button .md-button--primary }
