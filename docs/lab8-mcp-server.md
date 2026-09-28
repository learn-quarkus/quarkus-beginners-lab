# Lab 8: MCP Server — Expose Your Menu as an AI Tool

**Duration:** 10 minutes &nbsp;|&nbsp; **Projects:** `menu-mcp-server` (new) + `barista-bot` (from Lab 7)

!!! warning "Lab 7 required"
    This lab builds on `barista-bot` from Lab 7. If you didn't complete Lab 7, run the setup script below to get the starting point.

!!! info "What you'll build"
    - **`menu-mcp-server`** — a tiny Quarkus app on port `8084` that exposes menu data as **MCP tools** (`getMenuItems`, `getItemPrice`, `getItemsByMilkOption`)
    - **`barista-bot`** — updated to call those tools via the **MCP client** extension; the LLM automatically invokes the right tool when answering menu questions

    **Result:** Ask the barista bot *"What can I get with oat milk under $4?"* and it calls `getMenuItems()` over MCP, gets live structured data, and answers from it — no vector store, no embedding model, no similarity search.

    !!! warning "MCP tools and Easy RAG compete"
        If you leave Lab 7's Easy RAG enabled, the model gets `menu.txt` injected into the
        prompt and will usually answer from that instead of calling your tools. Step 6 tells
        you how to remove it — do that, or this lab won't demonstrate its own point.

!!! note "MCP in one sentence"
    **Model Context Protocol (MCP)** is an open standard for exposing tools, data, and prompts to AI assistants. Your Quarkus service becomes a tool server any LLM can call.

!!! tip "Want MCP pointing the other way?"
    **See [Lab 8B: Dev MCP](lab8b-dev-mcp.md)** — Quarkus dev mode is itself an MCP server, so
    your *coding assistant* can run your tests and read your live config. No code to write.

**Reference:** [Quarkus MCP Server docs](https://docs.quarkiverse.io/quarkus-mcp-server/dev/){ target="_blank" }

---

!!! tip "Working directory"
    All commands in this lab run from the `workshop/` folder inside the cloned repo. Make sure you are in that folder before you begin.

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
    Quarkus scans `@Tool` methods at build time, generates JSON schema for each parameter, and exposes them all over MCP — at `/mcp` (streamable HTTP) and `/mcp/sse` (legacy SSE). No manual registration, no routing code.

---

## Step 3 — Configure the server port

Open `src/main/resources/application.properties` and set the port so it doesn't clash with the other services:

```properties title="application.properties"
quarkus.http.port=8084
```

!!! warning "Why 8084 and not 8081?"
    Two things already want 8081:

    - `order-service` from Lab 4, if you still have it running.
    - Quarkus itself — **8081 is the default test port**. Leaving a server on it makes
      `quarkus build` / `mvn package` fail later (Lab 9) with `Port already bound: 8081`.

    Using 8084 from the start avoids both, and it's the same port Lab 10 expects.

---

## Step 4 — Start the MCP server

```bash
quarkus dev
```

The server starts on `http://localhost:8084`. You can verify the tools are exposed by opening the Dev UI:

**`http://localhost:8084/q/dev-ui`** → find **MCP Server – HTTP/SSE** → click **Tools**

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

!!! warning "Continuing from Lab 7? Remove Easy RAG first"
    If you added Easy RAG in Lab 7's bonus step, **take it back out before continuing** —
    otherwise this lab will not demonstrate what it claims to.

    Easy RAG injects chunks of `menu.txt` into the prompt as context on *every* request. The
    LLM then already has a plausible-looking menu in front of it, so it answers directly
    instead of calling your MCP tools. In testing, only one of the four questions in Step 8
    actually triggered a tool call; the rest were answered from the RAG context — including
    confidently offering an *"Americano with oat milk"*, which the MCP tool data does not
    support.

    Remove **both** the properties from `application.properties`:

    ```properties
    # Delete these two lines
    quarkus.langchain4j.easy-rag.path=rag-docs
    quarkus.langchain4j.easy-rag.path-type=CLASSPATH
    ```

    …and the extension from `pom.xml`:

    ```bash
    quarkus ext remove quarkus-langchain4j-easy-rag
    ```

    This matches the Lab 8 starting point produced by the setup script, which ships with no
    RAG configured. You can delete `src/main/resources/rag-docs/` too, though leaving the
    directory is harmless once the extension is gone.

Open `barista-bot/src/main/resources/application.properties` and add:

```properties
# MCP client — connect to menu-mcp-server on port 8084
quarkus.langchain4j.mcp.menu.transport-type=streamable-http   # (1)
quarkus.langchain4j.mcp.menu.url=http://localhost:8084/mcp    # (2)
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

Open **`http://localhost:8080`** and try these questions:

| Question | Tool the LLM is expected to pick |
|----------|----------------------------------|
| `What's on the menu?` | `getMenuItems()` |
| `How much is a Flat White?` | `getItemPrice("Flat White")` |
| `What can I get with oat milk under $4?` | `getMenuItems()`, then filters in its reasoning |
| `Do you have almond milk options?` | `getItemsByMilkOption("almond")` |

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

    1. **Easy RAG is still enabled** — see the warning in Step 6. This is by far the most common.
    2. **`menu-mcp-server` isn't running** on port 8084, so the tool list came back empty.
    3. The question was vague enough that the model felt it could answer without help — try
       `Which items can I get with almond milk?`, which is the most reliable trigger.

!!! note "MCP vs Easy RAG (from Lab 7)"
    | | Easy RAG | MCP tools |
    |--|----------|-----------|
    | Data source | Text file (`menu.txt`) | Live method call |
    | Accuracy | Fuzzy similarity search | Exact programmatic result |
    | Data updates | Requires app restart | Always fresh |
    | Setup | One property + one file | One annotation + one property |

    Both reduce hallucination — MCP tools give you **precision and freshness** at the cost of writing the tool methods.

    They can be combined in a real application, but doing so needs care: retrieved text and
    tool results are two competing sources of truth, and the model will happily prefer the
    one already sitting in its context. That's why this lab has you switch Easy RAG off
    rather than layer the two.

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

[← Lab 7: AI with LangChain4j](lab7-langchain4j.md){ .md-button }
[→ Lab 8B: Dev MCP](lab8b-dev-mcp.md){ .md-button }
[→ Lab 9: Containerize](lab9-containerize.md){ .md-button .md-button--primary }
