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

quarkus.swagger-ui.always-include=true
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

## Step 5 — Create the REST Endpoint

This endpoint powers both the Swagger UI (for testing) and the chat UI (for humans).

Create `src/main/java/org/coffee/ChatResource.java`:

```bash
touch src/main/java/org/coffee/ChatResource.java
```

Open `ChatResource.java` in your IDE and paste in the following:

```java
package org.coffee;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/chat")
public class ChatResource {

    @Inject
    BaristaAiService baristaAiService; // (1)

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String chat(@QueryParam("message") String message) { // (2)
        if (message == null || message.isBlank()) {
            return "Ask me anything about coffee! Try: ?message=What's a good morning coffee?";
        }
        return baristaAiService.chat(message);
    }
}
```

1. Inject the AI service like any other CDI bean — Quarkus generated the implementation.
2. `@QueryParam("message")` — the user's question arrives as a URL query parameter, e.g. `/chat?message=What+is+a+flat+white?`

---

## Step 6 — Add the Qute Chat UI

Instead of typing in Swagger UI, let's add a proper HTML form powered by **Qute** — Quarkus' server-side templating engine. Two files do the whole thing (`rest-qute` is already on the classpath from Step 2).

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

@Path("/")
public class ChatUiResource {

    @Inject
    Template chat;                       // (1)

    @Inject
    BaristaAiService baristaAiService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return chat.data("question", null, "reply", null); // (2)
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance ask(@FormParam("message") String message) { // (3)
        if (message == null || message.isBlank()) {
            return chat.data("question", null, "reply", null);
        }
        String reply = baristaAiService.chat(message.trim());
        return chat.data("question", message.trim(), "reply", reply);
    }
}
```

1. Quarkus injects the template by field name — `chat` maps to `src/main/resources/templates/chat.html` automatically.
2. `GET /` renders the form with no Q&A section (question and reply are null).
3. `POST /` calls the AI service and re-renders with `question` + `reply` populated — the input renders empty because there is no `value` attribute.

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
    body { font-family: -apple-system, "Segoe UI", sans-serif; max-width: 640px;
           margin: 3rem auto; padding: 0 1rem; color: #1f2328; }
    h1   { font-size: 1.4rem; margin-bottom: 0.25rem; }
    p.sub{ color: #57606a; margin-top: 0; margin-bottom: 2rem; font-size: 0.9rem; }
    .input-row { display: flex; gap: 0.5rem; }
    input[type=text] { flex: 1; padding: 0.55rem 0.75rem; font-size: 1rem;
                       border: 1px solid #d0d7de; border-radius: 6px; }
    button { padding: 0.55rem 1.1rem; font-size: 1rem; border: none;
             border-radius: 6px; cursor: pointer; }
    .btn-ask   { background: #3b82d4; color: #fff; }
    .btn-ask:hover { background: #2563be; }
    .btn-clear { background: #f7f8fa; color: #57606a; border: 1px solid #d0d7de; }
    .btn-clear:hover { background: #e5e7eb; }
    .qa    { margin-top: 2rem; display: flex; flex-direction: column; gap: 0.5rem; }
    .label { font-size: 0.75rem; font-weight: 600; color: #57606a; margin-bottom: 0.2rem; }
    .bubble{ padding: 0.75rem 1rem; border-radius: 6px; line-height: 1.6; }
    .q     { background: #dbeafe; }
    .a     { background: #f7f8fa; border: 1px solid #e5e7eb; white-space: pre-wrap; }
  </style>
</head>
<body>

  <h1>☕ Barista Bot</h1>
  <p class="sub">Powered by OpenAI + Quarkus LangChain4j. Ask me anything about coffee.</p>

  <form method="post" action="/">
    <div class="input-row">
      <input type="text" name="message" placeholder="e.g. What's in a flat white?" autofocus>
      <button type="submit" class="btn-ask">Ask</button>        <!-- (1) -->
      <a href="/"><button type="button" class="btn-clear">Clear</button></a>  <!-- (2) -->
    </div>
  </form>

  {#if question}                                                <!-- (3) -->
  <div class="qa">
    <div>
      <div class="label">You asked:</div>
      <div class="bubble q">{question}</div>
    </div>
    <div>
      <div class="label">Barista Bot says:</div>
      <div class="bubble a">{reply}</div>
    </div>
  </div>
  {/if}

</body>
</html>
```

1. **Ask** — submits the form via `POST /`; text box clears because the page re-renders with an empty `<input>`.
2. **Clear** — navigates to `GET /`, which returns `question=null` so the Q&A block disappears.
3. `{#if question}` — Q&A section only appears after a successful reply.

### Start and test

=== "Quarkus CLI"

    ```bash
    quarkus dev
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:dev
    ```

Open **`http://localhost:8080`** in your browser. Type a question and press **Ask**:

| Question | Example response |
|---------|-----------------|
| `What's a good coffee for a Monday morning?` | *"I'd recommend a double Espresso or a strong Flat White — both give you a bold caffeine kick to start the week."* |
| `How is cold brew made?` | *"Cold brew is made by steeping coarsely ground coffee in cold water for 12–24 hours."* |

The Swagger UI at `http://localhost:8080/q/swagger-ui` still works too — `GET /chat?message=...` is unchanged.

Watch the Dev Mode terminal — with `log-requests=true` you'll see the exact JSON payload sent to OpenAI and the response received.

---

## Step 7 — Bonus: Easy RAG

!!! info "Optional — add this if time allows"
    Easy RAG grounds the AI's responses in your actual menu document, preventing hallucination about menu items, prices, and ingredients.

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

## Summary

| What | How |
|------|-----|
| ✅ AI chatbot in ~20 lines | `@RegisterAiService` interface + `@SystemMessage` |
| ✅ No HTTP client boilerplate | Quarkus generates the CDI proxy |
| ✅ API key from environment | `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY` |
| ✅ Grounded responses (bonus) | `quarkus-langchain4j-easy-rag` + `menu.txt` |

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
        cd labs/lab7-langchain4j/solution && ./mvnw quarkus:dev

        # With RAG:
        cd labs/lab7-langchain4j/solution-with-rag && ./mvnw quarkus:dev
        ```

---

[← Lab 6: Fault Tolerance](lab6-fault-tolerance.md){ .md-button }
[→ Wrap-Up](wrap-up.md){ .md-button .md-button--primary }
