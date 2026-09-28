# Lab 8B: Dev MCP — Point Your AI Assistant at Quarkus

**Duration:** 10 minutes &nbsp;|&nbsp; **Project:** any Quarkus app in dev mode (examples use `menu-service`)

!!! info "What you'll do"
    Lab 8 pointed an LLM at **your** MCP server. This lab turns it around: **Quarkus dev mode is already an MCP server**, and you point your **AI coding assistant** at *it*.

    There is no dependency to add and no code to write. Dev mode exposes an MCP endpoint at `/q/dev-mcp`. Once your assistant is connected, it can run your tests, read and change configuration, inspect build-time data, and call tools contributed by whichever extensions you have installed — against the app running in front of you.

!!! warning "Experimental"
    Dev MCP is marked **experimental** in the Quarkus documentation. Tab labels and the exact tool list may change between releases. Needs **Quarkus 3.26+** — every project in this workshop qualifies.

**Reference:** [Quarkus Dev MCP guide](https://quarkus.io/guides/dev-mcp/){ target="_blank" } · [Quarkus Agent MCP guide](https://quarkus.io/guides/agent-mcp/){ target="_blank" }

---

## Two directions of MCP

Lab 8 and Lab 8B use the same protocol pointing opposite ways. Keeping them straight is most of the lesson:

| | Lab 8 — MCP server | Lab 8B — Dev MCP |
|--|--------------------|------------------|
| Who serves the tools | your `menu-mcp-server` | Quarkus dev mode itself |
| Who calls them | `barista-bot`'s LLM, at runtime | your coding assistant, while you develop |
| What you write | `@Tool` methods | nothing — it's built in |
| Where it runs | dev **and** production | dev mode only |
| What it's for | your users | you |

---

## Prerequisites

| You need | Why |
|----------|-----|
| A Quarkus app you can run in dev mode | The MCP endpoint only exists in dev mode |
| An MCP-capable AI coding assistant | Claude Code, Cursor, VS Code, Zed, JetBrains, Claude Desktop, Windsurf, Cline, Goose, OpenCode |
| [JBang](https://www.jbang.dev){ target="_blank" } | Runs the Agent MCP server in Step 3 |

Check JBang is available:

```bash
jbang version
```

---

## Step 1 — Start an app in dev mode

Any project from this workshop works. `menu-service` is the most interesting one to point an assistant at, because by Lab 3 it has entities, config profiles, health checks and tests for the assistant to actually look at:

=== "Quarkus CLI"

    ```bash
    cd labs/lab3-config-health/solution
    quarkus dev
    ```

=== "Maven"

    ```bash
    cd labs/lab3-config-health/solution
    mvn quarkus:dev
    ```

!!! note "Using a different project?"
    The Dev MCP endpoint follows the app's HTTP port. `menu-service` is on `8080`, so the endpoint is `http://localhost:8080/q/dev-mcp`. If you point this lab at `menu-mcp-server` from Lab 8 instead, it's on `8084` — so `http://localhost:8084/q/dev-mcp`.

---

## Step 2 — Enable Dev MCP in the Dev UI

Open the Dev UI:

```
http://localhost:8080/q/dev-ui
```

Then:

1. Open the **settings** dialog
2. Select the **Dev MCP** tab
3. **Enable** Dev MCP

The **Direct connection** section now shows the endpoint URL (default `http://localhost:8080/q/dev-mcp`) along with ready-made configuration snippets for each supported assistant.

!!! tip "This tab is your source of truth"
    The snippets in that dialog are generated for your actual port and your actual client list. If anything in this lab disagrees with what the dialog shows you, believe the dialog.

---

## Step 3 — Connect your assistant

The Quarkus guide recommends connecting through the **Agent MCP server** rather than wiring your client straight to `/q/dev-mcp`. Agent MCP proxies every Dev MCP tool from your running app *and* adds capabilities the raw endpoint has no way to offer:

- **Project scaffolding** — create new Quarkus apps with chosen extensions, started in dev mode
- **Application lifecycle** — start, stop, restart, tail logs, recover from crashes
- **Documentation search** — semantic RAG search over the Quarkus docs, version-matched to your project
- **Extension skills** — coding guidelines that extensions ship for AI agents

=== "Claude Code"

    ```bash
    claude mcp add quarkus-agent -- jbang quarkus-agent-mcp@quarkusio
    ```

=== "Other assistants"

    The **Dev MCP** tab in the Dev UI settings dialog generates the exact snippet for
    OpenCode, Cline, Goose, Zed, VS Code, Cursor, Claude Desktop, Windsurf and JetBrains IDEs.
    Copy it from there — it is generated against your running app, so the port is already correct.

The first run downloads the agent through JBang, so give it a moment. Restart your assistant afterwards if it doesn't pick up the new server straight away.

---

## Step 4 — Try it

With `menu-service` still running in dev mode, ask your assistant things it can only answer by calling into the live application:

| Ask | What it exercises |
|-----|-------------------|
| `What Quarkus extensions is this project using?` | Extension management |
| `Run the tests and tell me what failed.` | Continuous testing |
| `What is coffee.shop.name set to right now?` | Configuration access |
| `Set the log level for org.coffee to DEBUG.` | A tool that *mutates* the running app |
| `How do I add a Kafka consumer to this project?` | Agent MCP's documentation search |

The point to notice: the assistant is not reading your source files and guessing. It is calling into the JVM that is running, so the answers reflect live state — including config you changed in the Dev UI thirty seconds ago and never wrote to disk.

!!! warning "Your assistant can change things"
    Some Dev MCP tools write: update a config property, change a log level, restart the app. That is the feature, not a bug — but it means an assistant with this server attached can alter your running application without touching a file. Keep it pointed at local dev apps, and read the tool calls it proposes.

!!! note "A tool the assistant can't see?"
    Extension-contributed tools are **disabled by default**. Turn the ones you want on in the **Dev MCP** tab of the Dev UI settings dialog. Extensions can opt a tool into being on by default with `@DevMCPEnableByDefault`, but most don't.

---

## Step 5 — Direct connection (advanced)

If you'd rather skip JBang, any client supporting the **Streamable protocol, version 2025-03-26** can connect straight to the endpoint from Step 2. You lose application lifecycle management, documentation search and extension skills — you get the Dev MCP tools and nothing more.

The Quarkus guide treats this as the advanced path. Prefer Agent MCP unless you're building a custom integration.

---

## How extensions contribute tools

Worth knowing even if you never write an extension, because it explains why the tool list changes as you add dependencies.

A Dev MCP tool is just a JSON-RPC method with a description on it:

```java
public class MyExtensionRPCService {

    @JsonRpcDescription("Update a specific logger's level in this Quarkus application") // (1)
    public JsonObject updateLogLevel(
            @JsonRpcDescription("The logger name as defined in the logging implementation") String loggerName,
            @JsonRpcDescription("The new log level") String levelValue) {
        // implementation…
    }
}
```

1. `@JsonRpcDescription` is **mandatory** for Dev MCP. Without it the method still appears in the Dev UI, but is never exposed as an MCP tool.

All JSON-RPC methods show up in the Dev UI; only described ones reach Dev MCP. `@JsonRpcUsage` overrides that, taking `DEV_UI` and/or `DEV_MCP`.

This is a different contribution model from Lab 8 — `@Tool` builds *your own* MCP server, whereas `@JsonRpcDescription` adds a tool to the one Quarkus already runs for you in dev mode.

---

## Summary

| What | How |
|------|-----|
| ✅ MCP server for free | Built into dev mode — no dependency, no code |
| ✅ Enabled in one click | Dev UI → settings → **Dev MCP** tab |
| ✅ Assistant sees live state | Tools call into the running JVM, not your source files |
| ✅ Recommended client path | `claude mcp add quarkus-agent -- jbang quarkus-agent-mcp@quarkusio` |
| ✅ Dev-only by construction | The endpoint doesn't exist outside dev mode |

!!! tip "No solution folder for this one"
    Unlike the other labs there's nothing to catch up on — no project is created and no code is written. If it didn't work, the usual causes are Dev MCP still switched off in the Dev UI, the app not running in dev mode, or the assistant needing a restart after the config change.

---

[← Lab 8: MCP Server](lab8-mcp-server.md){ .md-button }
[→ Lab 9: Containerize](lab9-containerize.md){ .md-button .md-button--primary }
