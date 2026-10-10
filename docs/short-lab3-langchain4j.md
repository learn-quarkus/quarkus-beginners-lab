# Lab 3: AI Chatbot with LangChain4j

**Duration:** 8 minutes &nbsp;|&nbsp; **Project:** `barista-bot` (new)

!!! info "What you'll build"
    Build `barista-bot` — an AI-powered coffee shop assistant backed by OpenAI GPT-4o-mini.
    Define the entire chatbot as a **plain Java interface** with four annotations. Add conversation memory so the bot remembers each user's chat history across messages.

!!! warning "The Problem: Hallucination"
    LLMs are trained on vast internet data. When you ask about YOUR menu, the bot answers from its training data — confidently making up items, prices, and details that don't exist. It **hallucinates**. This lab demonstrates the problem; Lab 8 solves it with live data.

!!! warning "OpenAI API key required"
    Get your API key from [platform.openai.com](https://platform.openai.com).
    
    Create a `.env` file in your workshop root directory with:
    
    ```bash
    QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-...
    ```
    

**Extensions used:**

| Extension | Group ID | Purpose |
|-----------|----------|---------|
| `quarkus-langchain4j-openai` | `io.quarkiverse.langchain4j` | OpenAI integration via LangChain4j |
| `quarkus-rest-qute` | `io.quarkus` | Qute templates integrated with JAX-RS — powers the chat UI |

!!! warning "Different group ID"
    LangChain4j is a **Quarkiverse** extension — community maintained under the Quarkus umbrella.
    The group ID is `io.quarkiverse.langchain4j`, **not** `io.quarkus`.
    It also has its own BOM that must be added to `dependencyManagement`.

!!! tip "Working directory"
    All commands in this lab run from the `workshop/` folder inside the cloned repo. Make sure you are in that folder before you begin.

---

## Step 1 — Create the barista-bot Project

In a new terminal:

=== "Quarkus CLI"

    ```bash
    quarkus create app org.coffee:barista-bot \
      --extensions=rest-jackson,smallrye-openapi
    cd barista-bot
    ```

=== "Maven"

    ```bash
    mvn io.quarkus.platform:quarkus-maven-plugin:3.39.2:create \
      -DprojectGroupId=org.coffee \
      -DprojectArtifactId=barista-bot \
      -Dextensions=rest-jackson,smallrye-openapi
    cd barista-bot
    ```

!!! tip "Delete the generated sample files"
    ```bash
    rm src/main/java/org/coffee/GreetingResource.java
    rm src/test/java/org/coffee/GreetingResourceTest.java
    rm src/test/java/org/coffee/GreetingResourceIT.java
    ```

---

## Step 2 — Add LangChain4j Dependencies

In a terminal inside the `barista-bot` directory, run:

=== "Quarkus CLI"

    ```bash
    quarkus ext add quarkus-langchain4j-openai rest-qute
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:add-extension -Dextensions="quarkus-langchain4j-openai,rest-qute"
    ```

!!! note "What just happened?"
    `quarkus-langchain4j-openai` is registered in the Quarkus Platform registry so the full artifact ID works directly with `ext add` — no manual `pom.xml` edits needed. `rest-qute` is added at the same time so the Qute chat UI (Step 6) is ready to go.

---

## Step 3 — Configure the Application

Open `src/main/resources/application.properties` and add:

```properties title="application.properties"
# Run on port 8082 so menu-service can stay on 8080
quarkus.http.port=8082

# OpenAI model to use — gpt-4o-mini is fast and cost-effective
quarkus.langchain4j.openai.chat-model.model-name=gpt-4o-mini

# Log every request sent to OpenAI and every response received
# Disable in production — responses can be large
quarkus.langchain4j.log-requests=true
quarkus.langchain4j.log-responses=true
```

The API key is read from the `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY` environment variable automatically — no property needed in the file.

---

## Step 4 — Define the AI Service Interface

Create `src/main/java/org/coffee/BaristaAiService.java`:

```bash
mkdir -p src/main/java/org/coffee && touch src/main/java/org/coffee/BaristaAiService.java
```

Open `BaristaAiService.java` in your IDE and paste in the following:

```java
package org.coffee;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService  // (1)
@ApplicationScoped  // (2)
@SystemMessage("""
    You are a friendly and knowledgeable barista at The Quarkus Cafe.
    Answer questions about coffee, our menu, and brewing methods.
    Keep responses concise — 2-3 sentences maximum.
    If asked about something unrelated to coffee, politely redirect the conversation.
    """)   // (3)
public interface BaristaAiService {

    String chat(@MemoryId String memoryId, @UserMessage String message); // (4)
}
```

1. `@RegisterAiService` — tells Quarkus to generate a CDI bean implementation of this interface that calls the configured LLM.
2. `@ApplicationScoped` — makes the bean application-scoped. Default is `@RequestScoped`; use `@ApplicationScoped` when you want to share state (e.g., conversation memory) across requests.
3. `@SystemMessage` — the system prompt sent to the model before every user message. This is where you configure the AI's persona, constraints, and instructions.
4. `@MemoryId` and `@UserMessage` — `@MemoryId` is the session key for per-user conversation history. Quarkus maintains a separate `ChatMemory` for each unique `memoryId` (e.g., per browser session), so the LLM can remember previous messages in the conversation and answer follow-up questions contextually.

!!! note "What just happened?"
    That's the entire LLM integration. Quarkus reads the annotations at build time, generates a proxy CDI bean, creates an in-memory chat history store, and routes calls through the configured OpenAI client. When you call `baristaAiService.chat("user-123", "What's your best coffee?")`, Quarkus sends the system prompt + conversation history + user message to OpenAI, stores the response in memory, and returns it.

---

## Step 5 — Add the Qute Chat UI

Add a chat UI powered by **Qute** — Quarkus' server-side templating engine. Two files do the whole thing (`rest-qute` is already on the classpath from Step 2).

### Create the UI resource

Create `src/main/java/org/coffee/ChatUiResource.java`:

```bash
touch src/main/java/org/coffee/ChatUiResource.java
```

Open `ChatUiResource.java` in your IDE and paste in the following:

```java
package org.coffee;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/")
public class ChatUiResource {

    @Inject
    Template chat;

    @Inject
    BaristaAiService baristaAiService;

    // In-memory storage: sessionId → conversation history
    private Map<String, List<Map<String, String>>> conversations = new HashMap<>();

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(@CookieParam("sessionId") String sessionId) {
        if (sessionId != null) {
            conversations.remove(sessionId);
        }
        return chat.data("history", List.of());
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response ask(
            @FormParam("message") String message,
            @CookieParam("sessionId") String sessionId) {
        
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }

        // Get or create conversation history for this session
        List<Map<String, String>> history = conversations.computeIfAbsent(sessionId, k -> new ArrayList<>());

        if (message == null || message.isBlank()) {
            return Response.ok(chat.data("history", history))
                    .cookie(new NewCookie.Builder("sessionId").value(sessionId).path("/").build())
                    .build();
        }

        // Add user message to history
        history.add(Map.of("role", "user", "text", message.trim()));

        // Get bot response
        String reply = baristaAiService.chat(sessionId, message.trim());

        // Add bot response to history
        history.add(Map.of("role", "bot", "text", reply));

        return Response.ok(chat.data("history", history))
                .cookie(new NewCookie.Builder("sessionId").value(sessionId).path("/").build())
                .build();
    }
}
```

1. **In-memory Map** stores conversation history keyed by `sessionId`.
2. **Every message** is appended to the history list.
3. **Full history** is passed to the template and rendered.

!!! note "What just happened?"
    Qute resolves the `Template chat` injection by matching the field name to a file in `src/main/resources/templates/`. No path annotation needed — convention over configuration.

### Create the Qute template

```bash
mkdir -p src/main/resources/templates && touch src/main/resources/templates/chat.html
```

Open `chat.html` in your IDE and paste in the following:

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>The Quarkus Cafe — Barista Bot</title>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, "Segoe UI", system-ui, sans-serif;
      background: linear-gradient(135deg, #f0f2f5 0%, #e5e7eb 100%);
      min-height: 100vh;
      display: flex; align-items: center; justify-content: center; padding: 1rem;
    }
    .card {
      width: 100%; max-width: 560px; background: #fff;
      border-radius: 16px; box-shadow: 0 10px 40px rgba(0,0,0,0.1);
      display: flex; flex-direction: column; height: 620px; overflow: hidden;
    }
    .card-header {
      padding: 1.25rem; border-bottom: 1px solid #e5e7eb;
      display: flex; align-items: center; gap: 1rem; flex-shrink: 0;
      background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%);
    }
    .avatar {
      width: 44px; height: 44px; background: rgba(255,255,255,0.2);
      border-radius: 50%; display: flex; align-items: center;
      justify-content: center; font-size: 1.3rem;
    }
    .card-header h1 { font-size: 1.1rem; font-weight: 600; color: #fff; }
    .card-header p  { font-size: 0.78rem; color: rgba(255,255,255,0.8); }
    .messages {
      flex: 1; overflow-y: auto; padding: 1.5rem;
      display: flex; flex-direction: column; gap: 1rem;
      background: #fafbfc;
    }
    .messages::-webkit-scrollbar { width: 6px; }
    .messages::-webkit-scrollbar-track { background: transparent; }
    .messages::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 3px; }
    .empty { margin: auto; text-align: center; color: #8b949e; }
    .empty strong { display: block; font-size: 1.1rem; color: #57606a; margin-bottom: 0.5rem; }
    .row      { display: flex; flex-direction: column; max-width: 80%; animation: slideIn 0.3s ease; }
    @keyframes slideIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
    .row.user { align-self: flex-end;  align-items: flex-end; }
    .row.bot  { align-self: flex-start; align-items: flex-start; }
    .sender   { font-size: 0.7rem; font-weight: 600; color: #8b949e;
                margin-bottom: 4px; text-transform: uppercase; letter-spacing: 0.5px; }
    .bubble   { padding: 0.8rem 1rem; border-radius: 14px; font-size: 0.95rem;
                line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
    .row.user .bubble { background: #1d4ed8; color: #fff; border-bottom-right-radius: 4px; }
    .row.bot  .bubble { background: #fff; color: #1f2328; border: 1px solid #e5e7eb;
                        border-bottom-left-radius: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
    .typing { display: flex; gap: 4px; }
    .typing span { width: 8px; height: 8px; background: #8b949e;
                   border-radius: 50%; animation: bounce 1.4s infinite; }
    .typing span:nth-child(2) { animation-delay: 0.2s; }
    .typing span:nth-child(3) { animation-delay: 0.4s; }
    @keyframes bounce { 0%, 60%, 100% { transform: translateY(0); } 30% { transform: translateY(-10px); } }
    .input-bar  { padding: 1rem 1.5rem; border-top: 1px solid #e5e7eb; flex-shrink: 0; background: #fff; }
    .input-row  { display: flex; gap: 0.75rem; }
    input[type=text] {
      flex: 1; padding: 0.7rem 1rem; font-size: 0.95rem;
      border: 1px solid #d0d7de; border-radius: 8px; outline: none;
      transition: all 0.2s;
    }
    input[type=text]:focus { border-color: #1d4ed8; box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.1); }
    input[type=text]:disabled { background: #f0f2f5; color: #8b949e; cursor: not-allowed; }
    button { padding: 0.7rem 1.2rem; font-size: 0.9rem; border: none;
             border-radius: 8px; cursor: pointer; font-weight: 500; transition: all 0.2s; }
    .btn-send  { background: #1d4ed8; color: #fff; }
    .btn-send:hover:not(:disabled)  { background: #1e40af; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(29, 78, 216, 0.3); }
    .btn-send:disabled { background: #cbd5e1; cursor: not-allowed; }
    .btn-clear { background: transparent; color: #57606a; border: 1px solid #d0d7de; }
    .btn-clear:hover { background: #f7f8fa; }
  </style>
</head>
<body>
<div class="card">
  <div class="card-header">
    <div class="avatar">☕</div>
    <div>
      <h1>Barista Bot</h1>
      <p>Ask about coffee & our menu</p>
    </div>
  </div>
  <div class="messages" id="msgs">
    {#if history.size > 0}
      {#for msg in history}
        <div class="row {msg.role}">
          <span class="sender">{msg.role == 'user' ? 'You' : 'Barista Bot'}</span>
          <div class="bubble">{msg.text}</div>
        </div>
      {/for}
    {#else}
      <div class="empty">
        <strong>☕ Welcome to Barista Bot!</strong>
        <p>Ask me anything about coffee, menu items,<br>brewing methods, or recommendations.</p>
      </div>
    {/if}
  </div>
  <div class="input-bar">
    <form method="post" action="/">
      <div class="input-row">
        <input type="text" name="message" 
               placeholder="e.g. What's in a flat white?" autofocus required>
        <button type="submit" class="btn-send">Send</button>
        <a href="/"><button type="button" class="btn-clear">Clear</button></a>
      </div>
    </form>
  </div>
</div>

<script>
  const msgs = document.getElementById('msgs');
  msgs.scrollTop = msgs.scrollHeight;
</script>
</body>
</html>
```

### Start and test

=== "Quarkus CLI"

    ```bash
    quarkus dev
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:dev
    ```

Open **`http://localhost:8082`** in your browser. Type a question and press **Send**:

| Question | Example response |
|---------|-----------------|
| `What's a good coffee for a Monday morning?` | *"I'd recommend a double Espresso or a strong Flat White — both give you a bold caffeine kick to start the week."* |
| `How is cold brew made?` | *"Cold brew is made by steeping coarsely ground coffee in cold water for 12–24 hours."* |

Watch the Dev Mode terminal — with `log-requests=true` you'll see the exact JSON payload sent to OpenAI and the response received.

---


## Summary

| What | How |
|------|-----|
| ✅ AI chatbot with memory | `@RegisterAiService` + `@MemoryId` for per-session history |
| ✅ System prompt + persona | `@SystemMessage` defines the bot's personality |
| ✅ No HTTP client boilerplate | Quarkus generates the CDI proxy |
| ✅ API key from environment | `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY` env var |
| ✅ Web UI | Qute templates + cookie-based session tracking |


[← Lab 2: Kafka Messaging](short-lab2-kafka.md){ .md-button }
[→ Lab 4: MCP Server](short-lab4-mcp-server.md){ .md-button .md-button--primary }
