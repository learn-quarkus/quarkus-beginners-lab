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
    ```

---

## Step 2 — Add LangChain4j Dependencies

The Quarkus CLI cannot add Quarkiverse extensions directly, so edit `pom.xml` manually.

**First, add a version property** to the `<properties>` section of `pom.xml`, alongside the existing Quarkus platform version:

```xml title="pom.xml — properties section"
<quarkus.langchain4j.version>3.33.1</quarkus.langchain4j.version>
```

**Then add the LangChain4j BOM** inside the existing `<dependencyManagement>` section, after the Quarkus BOM entry:

```xml title="pom.xml — dependencyManagement section"
<dependency>
  <groupId>io.quarkus.platform</groupId>   <!-- (1) -->
  <artifactId>quarkus-langchain4j-bom</artifactId>
  <version>${quarkus.langchain4j.version}</version>  <!-- (2) -->
  <type>pom</type>
  <scope>import</scope>
</dependency>
```

1. As of Quarkus 3.20, LangChain4j is part of the **Quarkus Platform** — the BOM group is now `io.quarkus.platform`, not `io.quarkiverse.langchain4j`.
2. The BOM version tracks the Quarkus platform version. `3.33.1` matches Quarkus 3.33 LTS. Using a property makes it easy to update both BOMs in one place.

**Add the OpenAI runtime dependency** in the `<dependencies>` section:

```xml title="pom.xml — dependencies section"
<dependency>
  <groupId>io.quarkiverse.langchain4j</groupId>  <!-- runtime artifacts keep the quarkiverse groupId -->
  <artifactId>quarkus-langchain4j-openai</artifactId>
  <!-- version managed by the BOM above -->
</dependency>
```

!!! note "BOM vs runtime groupId"
    The **BOM** (`quarkus-langchain4j-bom`) is now published under `io.quarkus.platform` — same as the Quarkus core BOM.
    The **runtime extension JARs** (`quarkus-langchain4j-openai`, `quarkus-langchain4j-easy-rag`, etc.) still use `io.quarkiverse.langchain4j`.
    This is a common pattern in the Quarkus ecosystem — the platform BOM manages versions, the Quarkiverse group publishes the artifacts.

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

```java
package org.coffee;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService  // (1)
@ApplicationScoped  // (2)
@SystemMessage("""  // (3)
    You are a friendly and knowledgeable barista at The Quarkus Cafe.
    Answer questions about coffee, our menu, and brewing methods.
    Keep responses concise — 2-3 sentences maximum.
    If asked about something unrelated to coffee, politely redirect the conversation.
    """)
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

## Step 5 — Create the Chat Endpoint

Create `src/main/java/org/coffee/ChatResource.java`:

```bash
touch src/main/java/org/coffee/ChatResource.java
```

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

## Step 6 — Start and Test

=== "Quarkus CLI"

    ```bash
    quarkus dev
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:dev
    ```

Open `http://localhost:8080/q/swagger-ui` and find `GET /chat`.

Click **Try it out**, enter a message, and execute:

| message | Example response |
|---------|-----------------|
| `What's a good coffee for a Monday morning?` | *"I'd recommend a double Espresso or a strong Flat White — both give you a bold caffeine kick to start the week. If you prefer something smoother, a Cappuccino with its rich foam is a great choice!"* |
| `How is cold brew made?` | *"Cold brew is made by steeping coarsely ground coffee in cold water for 12-24 hours. The slow, cold extraction produces a smooth, low-acid concentrate that's naturally sweet."* |

Watch the `quarkus dev` terminal — with `log-requests=true` you'll see the exact JSON payload sent to OpenAI and the response received.

---

## Step 7 — Bonus: Easy RAG

!!! info "Optional — add this if time allows"
    Easy RAG grounds the AI's responses in your actual menu document, preventing hallucination about menu items, prices, and ingredients.

**Add the Easy RAG dependency** to `pom.xml`:

```xml
<dependency>
  <groupId>io.quarkiverse.langchain4j</groupId>
  <artifactId>quarkus-langchain4j-easy-rag</artifactId>
</dependency>
```

**Create the menu document** at `src/main/resources/menu.txt`:

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

**Add the config property** to `application.properties`:

```properties
# Easy RAG — ingest this file and use it to answer questions
quarkus.langchain4j.easy-rag.path=menu.txt
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
