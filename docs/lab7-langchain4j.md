# Lab 7: AI Chatbot with LangChain4j

**Duration:** 8 minutes &nbsp;|&nbsp; **Project:** `barista-bot` (new)

!!! info "What you'll build"
    Build `barista-bot` — an AI-powered coffee shop assistant backed by OpenAI GPT-4o-mini.
    Define the entire chatbot as a **plain Java interface** with four annotations. Then extend it with **Easy RAG** so it answers questions from your actual menu document — no hallucination, no vector database setup.

!!! warning "OpenAI API key required"
    This lab calls the OpenAI API. You need a key set in your terminal before running `quarkus dev`.

    **Instructor-led workshop:** your instructor will provide a key — use that.

    **Self-paced:** use your own key from [platform.openai.com](https://platform.openai.com).

    Set it in the terminal you will use for this lab (session only — not saved permanently):

    === "macOS / Linux"

        ```bash
        export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-...
        ```

    === "Windows (PowerShell)"

        ```powershell
        $env:QUARKUS_LANGCHAIN4J_OPENAI_API_KEY="sk-..."
        ```

**Extensions used:**

| Extension | Group ID | Purpose |
|-----------|----------|---------|
| `quarkus-langchain4j-openai` | `io.quarkiverse.langchain4j` | OpenAI integration via LangChain4j |
| `quarkus-rest-qute` | `io.quarkus` | Qute templates integrated with JAX-RS — powers the chat UI |
| `quarkus-langchain4j-easy-rag` | `io.quarkiverse.langchain4j` | Document ingestion + RAG (bonus step) |

!!! warning "Different group ID"
    LangChain4j is a **Quarkiverse** extension — community maintained under the Quarkus umbrella.
    The group ID is `io.quarkiverse.langchain4j`, **not** `io.quarkus`.
    It also has its own BOM that must be added to `dependencyManagement`.

---

## Background: What is LangChain4j?

!!! note "What is LangChain4j?"
    LangChain4j is a Java library for building LLM-powered applications. The Quarkus integration (`quarkus-langchain4j`) takes it further: you define your AI interaction as a **Java interface** annotated with `@RegisterAiService`, and Quarkus generates a CDI bean that calls the LLM API for you — with no HTTP client, no JSON parsing, and no API key in your business logic.

    ```java
    // This is the entire OpenAI integration:
    @RegisterAiService
    public interface BaristaAiService {
        String chat(@UserMessage String message);
    }
    ```

---

## Step 1 — Create the barista-bot Project

In a new terminal (separate from `menu-service`):

=== "Quarkus CLI"

    ```bash
    quarkus create app org.coffee:barista-bot \
      --extensions=rest-jackson,smallrye-openapi
    cd barista-bot
    ```

=== "Maven"

    ```bash
    mvn io.quarkus.platform:quarkus-maven-plugin:3.33.3:create \
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

    String chat(@UserMessage String message); // (4)
}
```

1. `@RegisterAiService` — tells Quarkus to generate a CDI bean implementation of this interface that calls the configured LLM.
2. `@ApplicationScoped` — makes the bean application-scoped. Default is `@RequestScoped`; use `@ApplicationScoped` when you want to share state (e.g., conversation memory) across requests.
3. `@SystemMessage` — the system prompt sent to the model before every user message. This is where you configure the AI's persona, constraints, and instructions.
4. `@UserMessage` on the parameter — marks this as the user's message to send to the model.

!!! note "What just happened?"
    That's the entire LLM integration. Quarkus reads the annotations at build time, generates a proxy CDI bean, and routes calls through the configured OpenAI client. When you call `baristaAiService.chat("What's your best coffee?")`, Quarkus sends the system prompt + user message to OpenAI and returns the response as a `String`.

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
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/")
public class ChatUiResource {

    @Inject
    Template chat;                       // (1)

    @Inject
    BaristaAiService baristaAiService;

    private final List<Map<String, String>> history = new ArrayList<>(); // (2)

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return chat.data("history", List.copyOf(history));
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance ask(@FormParam("message") String message) { // (3)
        if (message == null || message.isBlank()) {
            return chat.data("history", List.copyOf(history));
        }
        String reply = baristaAiService.chat(message.trim());
        history.add(Map.of("role", "user", "text", message.trim()));
        history.add(Map.of("role", "bot",  "text", reply));
        return chat.data("history", List.copyOf(history));
    }
}
```

1. Quarkus injects the template by field name — `chat` maps to `src/main/resources/templates/chat.html` automatically.
2. In-memory list keeps the conversation turns for the current dev session. Each turn is a `{role, text}` map matching the `{#for turn in history}` loop in the template.
3. `POST /` calls the AI service, appends both turns, and re-renders with the updated history.

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
      background: #f0f2f5; min-height: 100vh;
      display: flex; align-items: center; justify-content: center; padding: 1rem;
    }
    .card {                                               /* (1) */
      width: 100%; max-width: 560px; background: #fff;
      border-radius: 16px; border: 1px solid #e5e7eb;
      display: flex; flex-direction: column; height: 620px; overflow: hidden;
    }
    .card-header {
      padding: 1rem 1.25rem; border-bottom: 1px solid #e5e7eb;
      display: flex; align-items: center; gap: 0.65rem; flex-shrink: 0;
    }
    .avatar {
      width: 36px; height: 36px; background: #1d4ed8; border-radius: 50%;
      display: flex; align-items: center; justify-content: center; font-size: 1.1rem;
    }
    .card-header h1 { font-size: 1rem; font-weight: 600; color: #1f2328; }
    .card-header p  { font-size: 0.78rem; color: #57606a; }
    .messages {                                           /* (2) */
      flex: 1; overflow-y: auto; padding: 1rem 1.25rem;
      display: flex; flex-direction: column; gap: 0.85rem;
    }
    .empty { margin: auto; text-align: center; color: #8b949e; font-size: 0.85rem; line-height: 1.7; }
    .empty strong { display: block; font-size: 1.1rem; color: #57606a; margin-bottom: 0.3rem; }
    .row      { display: flex; flex-direction: column; max-width: 78%; }
    .row.user { align-self: flex-end;  align-items: flex-end; }   /* (3) */
    .row.bot  { align-self: flex-start; align-items: flex-start; }
    .sender   { font-size: 0.68rem; font-weight: 600; color: #8b949e;
                margin-bottom: 3px; text-transform: uppercase; }
    .bubble   { padding: 0.6rem 0.9rem; border-radius: 14px; font-size: 0.92rem;
                line-height: 1.55; white-space: pre-wrap; word-break: break-word; }
    .row.user .bubble { background: #1d4ed8; color: #fff; border-bottom-right-radius: 4px; }
    .row.bot  .bubble { background: #f7f8fa; color: #1f2328; border: 1px solid #e5e7eb;
                        border-bottom-left-radius: 4px; }
    .input-bar  { padding: 0.85rem 1.25rem; border-top: 1px solid #e5e7eb; flex-shrink: 0; }
    .input-row  { display: flex; gap: 0.5rem; }
    input[type=text] {
      flex: 1; padding: 0.55rem 0.85rem; font-size: 0.95rem;
      border: 1px solid #d0d7de; border-radius: 8px; outline: none;
    }
    input[type=text]:focus { border-color: #3b82d4; }
    button { padding: 0.55rem 1rem; font-size: 0.9rem; border: none;
             border-radius: 8px; cursor: pointer; font-weight: 500; }
    .btn-send  { background: #1d4ed8; color: #fff; }
    .btn-send:hover  { background: #1e40af; }
    .btn-clear { background: #f7f8fa; color: #57606a; border: 1px solid #d0d7de; }
    .btn-clear:hover { background: #e5e7eb; }
  </style>
</head>
<body>
<div class="card">
  <div class="card-header">
    <div class="avatar">☕</div>
    <div>
      <h1>Barista Bot</h1>
      <p>Powered by OpenAI · Quarkus LangChain4j</p>
    </div>
  </div>
  <div class="messages" id="msgs">
    {#if history}
      {#for turn in history}
        {#if turn.role == "user"}
        <div class="row user">
          <span class="sender">You</span>
          <div class="bubble">{turn.text}</div>         <!-- (4) -->
        </div>
        {#else}
        <div class="row bot">
          <span class="sender">Barista Bot</span>
          <div class="bubble">{turn.text}</div>
        </div>
        {/if}
      {/for}
    {#else}
      <div class="empty">
        <strong>☕ Welcome!</strong>
        Ask me anything about coffee, our menu,<br>or how your favourite drink is made.
      </div>
    {/if}
  </div>
  <div class="input-bar">
    <form method="post" action="/">
      <div class="input-row">
        <input type="text" name="message"
               placeholder="e.g. What's in a flat white?" autofocus>
        <button type="submit" class="btn-send">Send</button>   <!-- (5) -->
        <a href="/"><button type="button" class="btn-clear">Clear</button></a>
      </div>
    </form>
  </div>
</div>
</body>
</html>
```

1. `.card` — a fixed-height flexbox column: header + scrollable messages + input bar.
2. `.messages` — `flex: 1` so it fills remaining height; `overflow-y: auto` makes it scroll when the conversation grows.
3. `.row.user` right-aligns your messages; `.row.bot` left-aligns the bot's — standard chat bubble layout.
4. Both bubble types share the `.bubble` class; colour is set by the parent `.row.user` / `.row.bot` selector.
5. **Send** submits; **Clear** navigates to `GET /` which resets the history.

### Start and test

=== "Quarkus CLI"

    ```bash
    quarkus dev
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:dev
    ```

Open **`http://localhost:8080`** in your browser. Type a question and press **Send**:

| Question | Example response |
|---------|-----------------|
| `What's a good coffee for a Monday morning?` | *"I'd recommend a double Espresso or a strong Flat White — both give you a bold caffeine kick to start the week."* |
| `How is cold brew made?` | *"Cold brew is made by steeping coarsely ground coffee in cold water for 12–24 hours."* |

Watch the Dev Mode terminal — with `log-requests=true` you'll see the exact JSON payload sent to OpenAI and the response received.

---

## Step 6 — Bonus: Easy RAG

!!! info "Optional — add this if time allows"
    Easy RAG grounds the AI's responses in your actual menu document, preventing hallucination about menu items, prices, and ingredients.

### Before you add RAG — see the problem first

With Dev Mode still running, open **`http://localhost:8080`** and ask:


```bash
Do you have oat milk?
```

The model will answer from its training data — it may guess, make up a price, or say something that doesn't match your actual menu. Note the response.

Now add RAG and ask the same question again at the end of this step to see the difference.

---

**Add the Easy RAG extension.** In a second terminal inside the `barista-bot` directory, run:

=== "Quarkus CLI"

    ```bash
    quarkus ext add io.quarkiverse.langchain4j:quarkus-langchain4j-easy-rag
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:add-extension -Dextensions="io.quarkiverse.langchain4j:quarkus-langchain4j-easy-rag"
    ```

**Create the menu document** at `src/main/resources/rag-docs/menu.txt`:

!!! note "Why a directory and CLASSPATH?"
    `easy-rag.path` must point to a **directory** — Easy RAG scans all files inside it. It also resolves the path on the **filesystem** by default; setting `path-type=CLASSPATH` tells it to look inside `src/main/resources` instead. Without both, you get `IllegalArgumentException`.

```bash
mkdir -p src/main/resources/rag-docs && touch src/main/resources/rag-docs/menu.txt
```

Open `menu.txt` in your IDE and paste in the following:

```text title="menu.txt"
THE QUARKUS CAFE — FULL MENU

=== HOT DRINKS ===
Espresso — $2.50: Concentrated shot, rich and bold. Dairy-free.
Cappuccino — $3.75: Espresso with steamed milk and thick foam. Available with oat milk.
Flat White — $4.00: Two espresso shots with velvety microfoam.
Latte — $4.25: Espresso with steamed milk. Available in vanilla, caramel, hazelnut.
Americano — $3.00: Espresso diluted with hot water.

=== COLD DRINKS ===
Cold Brew — $4.00: 12-hour cold-steeped. Smooth, low-acid. Available with oat milk.
Iced Latte — $4.50: Espresso over ice with cold milk.

=== MILK OPTIONS ===
Whole milk (included), Oat milk (+$0.50), Almond milk (+$0.50), Soy milk (+$0.50)
```

**Add the config properties** to `application.properties`:

```properties
# Easy RAG — directory inside src/main/resources; path-type=CLASSPATH required
quarkus.langchain4j.easy-rag.path=rag-docs
quarkus.langchain4j.easy-rag.path-type=CLASSPATH
```

Save all files. Quarkus live-reloads. Now ask the chatbot:

| Question (before RAG) | Question (after RAG) |
|-----------------------|---------------------|
| *"Do you have oat milk?"* → model may hallucinate | *"Do you have oat milk?"* → **"Yes! Oat milk is available as a milk option for an extra $0.50."** |
| *"What's your cheapest drink?"* → guess | *"What's your cheapest drink?"* → **"Our Espresso at $2.50 is the most affordable option."** |

!!! note "What Easy RAG does under the hood"
    When `quarkus-langchain4j-easy-rag` starts, it:

    1. Reads `menu.txt` and splits it into chunks
    2. Generates vector embeddings for each chunk (using OpenAI's embedding API)
    3. Stores them in an in-memory vector store

    When a question arrives:

    1. Embeds the question into a vector
    2. Finds the most semantically similar chunks from the store (similarity search)
    3. Injects them into the prompt as context before sending to the LLM

    The LLM now answers based on **your document** — not its training data. This eliminates hallucination for menu-specific facts.

---

## Step 7 — Bonus: Conversation Memory with `@MemoryId`

!!! info "Optional — add this if time allows"
    Right now every message is stateless — the bot has no memory of previous turns. Add `@MemoryId` to give each browser session its own conversation history, so the bot can say *"As I mentioned earlier…"* and follow-up questions work naturally.

### How it works

LangChain4j keeps a per-key `ChatMemory` (a sliding window of recent messages). You add a `@MemoryId` parameter to the AI service method and pass a stable ID per user/session. Quarkus stores the memory in-process (no database needed).

### Update the AI Service

Open `BaristaAiService.java` and replace the file with the following to add the `@MemoryId` parameter:

```java
package org.coffee;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService  // (1)
@ApplicationScoped
@SystemMessage("""
    You are a friendly and knowledgeable barista at The Quarkus Cafe.
    Answer questions about coffee, our menu, and brewing methods.
    Keep responses concise — 2-3 sentences maximum.
    If asked about something unrelated to coffee, politely redirect the conversation.
    """)
public interface BaristaAiService {

    String chat(@MemoryId String memoryId, @UserMessage String message); // (2)
}
```

1. No other annotation needed — Quarkus automatically creates an in-memory `ChatMemoryStore` keyed by `memoryId`.
2. `@MemoryId` — the value you pass here is the key for the per-session message history. Same value = same conversation thread.

### Update the UI Resource

Open `ChatUiResource.java` and replace its contents with:

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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Path("/")
public class ChatUiResource {

    @Inject
    Template chat;

    @Inject
    BaristaAiService baristaAiService;

    // In-memory history store: sessionId → list of {role, text} pairs  // (1)
    private final Map<String, List<Map<String, String>>> sessions = new ConcurrentHashMap<>();

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(@CookieParam("session") String session) { // (2)
        List<Map<String, String>> history = session != null
                ? sessions.getOrDefault(session, List.of())
                : List.of();
        return chat.data("history", history);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response ask(
            @CookieParam("session") String session,
            @FormParam("message") String message) {

        if (message == null || message.isBlank()) {
            return Response.ok(chat.data("history", List.of())
                    .render()).type(MediaType.TEXT_HTML).build();
        }

        // Create a new session ID if this is the first message  // (3)
        String sessionId = (session != null && !session.isBlank()) ? session : UUID.randomUUID().toString();

        String reply = baristaAiService.chat(sessionId, message.trim()); // (4)

        List<Map<String, String>> history =
                sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
        history.add(Map.of("role", "user", "text", message.trim()));
        history.add(Map.of("role", "bot",  "text", reply));

        NewCookie cookie = new NewCookie.Builder("session")    // (5)
                .value(sessionId).path("/").build();

        return Response.ok(chat.data("history", history).render())
                .type(MediaType.TEXT_HTML)
                .cookie(cookie)
                .build();
    }
}
```

1. A simple in-process map holds the display history for the UI. LangChain4j keeps its own token-window copy for the LLM — both are keyed by the same `sessionId`.
2. The session cookie is read on every request. If it's absent, we treat this as a new conversation.
3. On the first `POST`, a UUID is minted and written back as a cookie so every subsequent request reuses the same key.
4. `sessionId` is passed as `@MemoryId` — LangChain4j automatically appends the new turn to the stored `ChatMemory` for that key before calling the model.
5. The cookie is returned in the response header. The browser stores it and sends it automatically on the next request.

### Update the Qute Template

Open `src/main/resources/templates/chat.html` and replace its contents with:

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
      background: #f0f2f5; min-height: 100vh;
      display: flex; align-items: center; justify-content: center; padding: 1rem;
    }
    .card {
      width: 100%; max-width: 560px; background: #fff;
      border-radius: 16px; border: 1px solid #e5e7eb;
      display: flex; flex-direction: column; height: 620px; overflow: hidden;
    }
    .card-header {
      padding: 1rem 1.25rem; border-bottom: 1px solid #e5e7eb;
      display: flex; align-items: center; gap: 0.65rem; flex-shrink: 0;
    }
    .avatar {
      width: 36px; height: 36px; background: #1d4ed8; border-radius: 50%;
      display: flex; align-items: center; justify-content: center; font-size: 1.1rem;
    }
    .card-header h1 { font-size: 1rem; font-weight: 600; color: #1f2328; }
    .card-header p  { font-size: 0.78rem; color: #57606a; }
    .messages {
      flex: 1; overflow-y: auto; padding: 1rem 1.25rem;
      display: flex; flex-direction: column; gap: 0.85rem;
    }
    .empty { margin: auto; text-align: center; color: #8b949e; font-size: 0.85rem; line-height: 1.7; }
    .empty strong { display: block; font-size: 1.1rem; color: #57606a; margin-bottom: 0.3rem; }
    .row      { display: flex; flex-direction: column; max-width: 78%; }
    .row.user { align-self: flex-end;  align-items: flex-end; }
    .row.bot  { align-self: flex-start; align-items: flex-start; }
    .sender   { font-size: 0.68rem; font-weight: 600; color: #8b949e;
                margin-bottom: 3px; text-transform: uppercase; }
    .bubble   { padding: 0.6rem 0.9rem; border-radius: 14px; font-size: 0.92rem;
                line-height: 1.55; white-space: pre-wrap; word-break: break-word; }
    .row.user .bubble { background: #1d4ed8; color: #fff; border-bottom-right-radius: 4px; }
    .row.bot  .bubble { background: #f7f8fa; color: #1f2328; border: 1px solid #e5e7eb;
                        border-bottom-left-radius: 4px; }
    .input-bar  { padding: 0.85rem 1.25rem; border-top: 1px solid #e5e7eb; flex-shrink: 0; }
    .input-row  { display: flex; gap: 0.5rem; }
    input[type=text] {
      flex: 1; padding: 0.55rem 0.85rem; font-size: 0.95rem;
      border: 1px solid #d0d7de; border-radius: 8px; outline: none;
    }
    input[type=text]:focus { border-color: #3b82d4; }
    button { padding: 0.55rem 1rem; font-size: 0.9rem; border: none;
             border-radius: 8px; cursor: pointer; font-weight: 500; }
    .btn-send  { background: #1d4ed8; color: #fff; }
    .btn-send:hover  { background: #1e40af; }
    .btn-clear { background: #f7f8fa; color: #57606a; border: 1px solid #d0d7de; }
    .btn-clear:hover { background: #e5e7eb; }
  </style>
</head>
<body>
<div class="card">
  <div class="card-header">
    <div class="avatar">☕</div>
    <div>
      <h1>Barista Bot</h1>
      <p>Powered by OpenAI · Quarkus LangChain4j</p>
    </div>
  </div>
  <div class="messages">
    {#if history}
      {#for turn in history}
        {#if turn.role == "user"}
        <div class="row user">
          <span class="sender">You</span>
          <div class="bubble">{turn.text}</div>
        </div>
        {#else}
        <div class="row bot">
          <span class="sender">Barista Bot</span>
          <div class="bubble">{turn.text}</div>
        </div>
        {/if}
      {/for}
    {#else}
      <div class="empty">
        <strong>☕ Welcome!</strong>
        Ask me anything about coffee, our menu,<br>or how your favourite drink is made.
      </div>
    {/if}
  </div>
  <div class="input-bar">
    <form method="post" action="/">
      <div class="input-row">
        <input type="text" name="message"
               placeholder="e.g. What's in a flat white?" autofocus>
        <button type="submit" class="btn-send">Send</button>
        <a href="/"><button type="button" class="btn-clear">Clear</button></a>
      </div>
    </form>
  </div>
</div>
</body>
</html>
```

### Test the memory

With Dev Mode still running, open **`http://localhost:8080`** and try a multi-turn conversation:

| Turn | You type | What you'll see |
|------|----------|-----------------|
| 1 | `What's in a flat white?` | Explanation of espresso + microfoam |
| 2 | `How does that compare to a cappuccino?` | Comparison — bot remembers you asked about a flat white |
| 3 | `Which one has more milk?` | Correct answer referencing both drinks from earlier turns |

Click **Clear** to start a fresh conversation (a new session cookie is minted on the next `POST`).

!!! note "Memory window"
    By default, LangChain4j keeps the last **10 messages** (5 turns) in memory. You can tune this in `application.properties`:

    ```properties
    quarkus.langchain4j.chat-memory.max-messages=20
    ```

---

## Summary

| What | How |
|------|-----|
| ✅ AI chatbot in ~20 lines | `@RegisterAiService` interface + `@SystemMessage` |
| ✅ No HTTP client boilerplate | Quarkus generates the CDI proxy |
| ✅ API key from environment | `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY` |
| ✅ Grounded responses (bonus) | `quarkus-langchain4j-easy-rag` + `menu.txt` |
| ✅ Multi-turn memory (bonus) | `@MemoryId` parameter + cookie-based session |

!!! tip "Stuck or fell behind?"
    Two complete solutions are available:

    === "Quarkus CLI"
        ```bash
        # Without RAG:
        cd labs/lab7-langchain4j/solution && quarkus dev

        # With RAG:
        cd labs/lab7-langchain4j/solution-with-rag && quarkus dev
        ```
    === "Maven"
        ```bash
        # Without RAG:
        cd labs/lab7-langchain4j/solution && mvn quarkus:dev

        # With RAG:
        cd labs/lab7-langchain4j/solution-with-rag && mvn quarkus:dev
        ```

---

[← Lab 6: Fault Tolerance](lab6-fault-tolerance.md){ .md-button }
[→ Lab 8: MCP Server](lab8-mcp-server.md){ .md-button .md-button--primary }
