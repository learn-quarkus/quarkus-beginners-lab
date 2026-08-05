# Lab 7B: Alternative LLM Providers

**Duration:** 8 minutes &nbsp;|&nbsp; **Project:** `barista-bot` (new, with your choice of provider)

!!! info "What you'll build"
    Build `barista-bot` — an AI-powered coffee shop assistant backed by your choice of LLM provider: **OpenAI GPT-4o-mini**, **LlamaCloud free tier**, or **IBM Watson X AI**. The same barista interface works with all three — only dependencies and configuration change.

!!! tip "Key takeaway"
    This lab demonstrates LangChain4j's abstraction power. Your AI service code is identical regardless of which provider you choose. Only the pom.xml and application.properties differ.

---


## Prerequisites: Get Your API Key

Choose your provider and follow the setup instructions below.

=== "OpenAI"

    **Sign up & get API key:**

    1. Visit [platform.openai.com](https://platform.openai.com)
    2. Create an account or sign in
    3. Navigate to **API keys** → **Create new secret key**
    4. Copy the key (starts with `sk-`)
    
    **Set environment variable in your terminal (session only):**

    === "macOS / Linux"

        ```bash
        export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-...
        ```

    === "Windows (PowerShell)"

        ```powershell
        $env:QUARKUS_LANGCHAIN4J_OPENAI_API_KEY="sk-..."
        ```

=== "LlamaCloud"

    **Sign up for free tier:**

    1. Visit [llamaindex](https://login.llamaindex.ai/) and sign up for free
    2. Create a new project in the LlamaCloud console
    3. Generate an API key in your account settings
    4. Copy the key
    
    **Set environment variable in your terminal (session only):**

    === "macOS / Linux"

        ```bash
        export QUARKUS_LANGCHAIN4J_OLLAMA_API_KEY=your-api-key
        ```

    === "Windows (PowerShell)"

        ```powershell
        $env:QUARKUS_LANGCHAIN4J_OLLAMA_API_KEY="your-api-key"
        ```
    
    !!! note "Using Ollama extension"
        LlamaCloud integrates with Quarkus via the Ollama extension (which supports any Ollama-compatible API).

=== "Watson X AI"

    **Sign up for free trial:**

    1. Visit [ibm.com/cloud/watsonx/ai](https://www.ibm.com/cloud/watsonx/ai)
    2. Setup a free trial
    3. Create IBM Cloud account and authenticate
    4. In Watson Studio, generate an API key under **Access control** → **API keys**
    5. Note your **API Key** and **Project ID** (or Space ID)
    
    **Set environment variables in your terminal (session only):**

    === "macOS / Linux"

        ```bash
        export QUARKUS_LANGCHAIN4J_WATSONX_API_KEY=your-api-key
        export QUARKUS_LANGCHAIN4J_WATSONX_PROJECT_ID=your-project-id
        export QUARKUS_LANGCHAIN4J_WATSONX_BASE_URL=https://us-south.ml.cloud.ibm.com
        ```

    === "Windows (PowerShell)"

        ```powershell
        $env:QUARKUS_LANGCHAIN4J_WATSONX_API_KEY="your-api-key"
        $env:QUARKUS_LANGCHAIN4J_WATSONX_PROJECT_ID="your-project-id"
        $env:QUARKUS_LANGCHAIN4J_WATSONX_BASE_URL="https://us-south.ml.cloud.ibm.com"
        ```

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

Choose your provider and add the corresponding extension:

=== "OpenAI"

    **Quarkus CLI:**
    ```bash
    quarkus ext add quarkus-langchain4j-openai rest-qute
    ```

    **Maven:**
    ```bash
    ./mvnw quarkus:add-extension -Dextensions="quarkus-langchain4j-openai,rest-qute"
    ```

=== "LlamaCloud"

    **Quarkus CLI:**
    ```bash
    quarkus ext add quarkus-langchain4j-ollama rest-qute
    ```

    **Maven:**
    ```bash
    ./mvnw quarkus:add-extension -Dextensions="quarkus-langchain4j-ollama,rest-qute"
    ```

=== "Watson X AI"

    **Quarkus CLI:**
    ```bash
    quarkus ext add quarkus-langchain4j-watsonx rest-qute
    ```

    **Maven:**
    ```bash
    ./mvnw quarkus:add-extension -Dextensions="quarkus-langchain4j-watsonx,rest-qute"
    ```

---

## Step 3 — Configure the Application

Open `src/main/resources/application.properties` and add the provider-specific configuration:

=== "OpenAI"

    ```properties title="application.properties"
    # OpenAI model config
    quarkus.langchain4j.openai.chat-model.model-name=gpt-4o-mini
    
    # Log requests/responses for learning — disable in production
    quarkus.langchain4j.log-requests=true
    quarkus.langchain4j.log-responses=true
    
    ```

    The API key is read from `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY` environment variable automatically.

=== "LlamaCloud"

    ```properties title="application.properties"
    # LlamaCloud (Ollama-compatible) config
    quarkus.langchain4j.ollama.chat-model.model-name=llama2
    quarkus.langchain4j.ollama.base-url=https://api.llamaindex.ai/ollama
    
    # Log requests/responses for learning — disable in production
    quarkus.langchain4j.log-requests=true
    quarkus.langchain4j.log-responses=true
    
    ```

    The API key is read from `QUARKUS_LANGCHAIN4J_OLLAMA_API_KEY` environment variable automatically.

=== "Watson X AI"

    ```properties title="application.properties"
    # Watson X AI config
    quarkus.langchain4j.watsonx.chat-model.model-name=meta-llama/llama-2-70b-chat
    quarkus.langchain4j.watsonx.version=2024-12-19
    
    # Log requests/responses for learning — disable in production
    quarkus.langchain4j.log-requests=true
    quarkus.langchain4j.log-responses=true
    
    ```

    API credentials are read from environment variables:
    - `QUARKUS_LANGCHAIN4J_WATSONX_API_KEY`
    - `QUARKUS_LANGCHAIN4J_WATSONX_PROJECT_ID` (or `SPACE_ID`)
    - `QUARKUS_LANGCHAIN4J_WATSONX_BASE_URL`

---

## Step 4 — Define the AI Service Interface

Create `src/main/java/org/coffee/BaristaAiService.java`:

```bash
mkdir -p src/main/java/org/coffee && touch src/main/java/org/coffee/BaristaAiService.java
```

This code is **identical for all three providers** — LangChain4j abstracts the provider:

```java
package org.coffee;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
@SystemMessage("""
    You are a friendly and knowledgeable barista at The Quarkus Cafe.
    Answer questions about coffee, our menu, and brewing methods.
    Keep responses concise — 2-3 sentences maximum.
    If asked about something unrelated to coffee, politely redirect the conversation.
    """)
public interface BaristaAiService {

    String chat(@UserMessage String message);
}
```

---

## Step 5 — Add the Qute Chat UI

### Create the UI resource

Create `src/main/java/org/coffee/ChatUiResource.java`:

```java
package org.coffee;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestQuery;

@Path("/chat")
public class ChatUiResource {

    @Inject
    Template chat;

    @Inject
    BaristaAiService baristaAi;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return chat.instance();
    }

    @POST
    @Path("/ask")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ask(String message) {
        return baristaAi.chat(message);
    }
}
```

### Create the Qute template

Create `src/main/resources/templates/chat.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quarkus Cafe Barista</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }
        .container {
            background: white;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
            width: 100%;
            max-width: 500px;
            display: flex;
            flex-direction: column;
            height: 90vh;
            max-height: 700px;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 12px 12px 0 0;
            text-align: center;
        }
        .header h1 {
            font-size: 24px;
            margin-bottom: 5px;
        }
        .header p {
            font-size: 12px;
            opacity: 0.9;
        }
        .chat-messages {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
            background: #f5f5f5;
        }
        .message {
            margin-bottom: 15px;
            animation: slideIn 0.3s ease;
        }
        @keyframes slideIn {
            from {
                opacity: 0;
                transform: translateY(10px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        .user-message {
            text-align: right;
        }
        .user-message .bubble {
            background: #667eea;
            color: white;
            padding: 10px 15px;
            border-radius: 18px;
            display: inline-block;
            max-width: 80%;
            word-wrap: break-word;
        }
        .barista-message .bubble {
            background: white;
            color: #333;
            padding: 10px 15px;
            border-radius: 18px;
            display: inline-block;
            max-width: 80%;
            word-wrap: break-word;
            border: 1px solid #e0e0e0;
        }
        .input-area {
            padding: 20px;
            border-top: 1px solid #e0e0e0;
            display: flex;
            gap: 10px;
        }
        .input-area input {
            flex: 1;
            border: 1px solid #ddd;
            border-radius: 24px;
            padding: 10px 15px;
            font-size: 14px;
            outline: none;
            transition: border-color 0.3s;
        }
        .input-area input:focus {
            border-color: #667eea;
        }
        .input-area button {
            background: #667eea;
            color: white;
            border: none;
            border-radius: 24px;
            padding: 10px 20px;
            cursor: pointer;
            font-size: 14px;
            transition: background 0.3s;
        }
        .input-area button:hover {
            background: #5568d3;
        }
        .input-area button:disabled {
            background: #ccc;
            cursor: not-allowed;
        }
        .typing-indicator {
            display: flex;
            gap: 4px;
            padding: 10px 15px;
        }
        .typing-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: #999;
            animation: typing 1.4s infinite;
        }
        .typing-dot:nth-child(2) {
            animation-delay: 0.2s;
        }
        .typing-dot:nth-child(3) {
            animation-delay: 0.4s;
        }
        @keyframes typing {
            0%, 60%, 100% {
                opacity: 0.5;
                transform: translateY(0);
            }
            30% {
                opacity: 1;
                transform: translateY(-10px);
            }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>☕ Quarkus Cafe</h1>
            <p>Ask me anything about coffee!</p>
        </div>
        <div class="chat-messages" id="messages"></div>
        <div class="input-area">
            <input type="text" id="messageInput" placeholder="Ask about coffee..." />
            <button id="sendBtn" onclick="sendMessage()">Send</button>
        </div>
    </div>

    <script>
        const messagesDiv = document.getElementById('messages');
        const messageInput = document.getElementById('messageInput');
        const sendBtn = document.getElementById('sendBtn');

        function sendMessage() {
            const message = messageInput.value.trim();
            if (!message) return;

            // Display user message
            addMessage(message, 'user');
            messageInput.value = '';
            sendBtn.disabled = true;

            // Show typing indicator
            showTypingIndicator();

            // Send to backend
            fetch('/chat/ask', {
                method: 'POST',
                body: message,
                headers: {
                    'Content-Type': 'text/plain'
                }
            })
            .then(response => response.text())
            .then(reply => {
                removeTypingIndicator();
                addMessage(reply, 'barista');
                sendBtn.disabled = false;
                messageInput.focus();
            })
            .catch(error => {
                removeTypingIndicator();
                console.error('Error:', error);
                addMessage('Sorry, something went wrong. Please try again.', 'barista');
                sendBtn.disabled = false;
            });
        }

        function addMessage(text, sender) {
            const messageDiv = document.createElement('div');
            messageDiv.className = `message ${sender}-message`;
            messageDiv.innerHTML = `<div class="bubble">${escapeHtml(text)}</div>`;
            messagesDiv.appendChild(messageDiv);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }

        function showTypingIndicator() {
            const typingDiv = document.createElement('div');
            typingDiv.id = 'typing-indicator';
            typingDiv.className = 'message barista-message';
            typingDiv.innerHTML = `
                <div class="typing-indicator">
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                </div>
            `;
            messagesDiv.appendChild(typingDiv);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }

        function removeTypingIndicator() {
            const typingIndicator = document.getElementById('typing-indicator');
            if (typingIndicator) typingIndicator.remove();
        }

        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') sendMessage();
        });

        messageInput.focus();
    </script>
</body>
</html>
```

### Start and test

Run the application:

```bash
quarkus dev
```

Open your browser to [http://localhost:8080/chat](http://localhost:8080/chat)

Start chatting with the barista! Ask questions like:
- "What's the best way to make espresso?"
- "Tell me about your menu"
- "How do I make a cappuccino?"

---

## Step 6 — Bonus: Easy RAG

Add structured documents so the barista answers from your menu, not hallucinations.

### Add the Easy RAG extension

=== "Quarkus CLI"

    ```bash
    quarkus ext add quarkus-langchain4j-easy-rag
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:add-extension -Dextensions="quarkus-langchain4j-easy-rag"
    ```

### Create the menu document

Create `src/main/resources/rag-docs/menu.txt`:

```text
The Quarkus Cafe Menu

COFFEE
- Espresso: Single shot (1 oz), double shot (2 oz). Bold and concentrated.
- Americano: Espresso with hot water. 6 oz or 10 oz available.
- Latte: Espresso with steamed milk and foam. Smooth and creamy.
- Cappuccino: Equal parts espresso, steamed milk, and foam. Strong coffee flavor.
- Macchiato: Espresso marked with a small amount of milk foam.
- Flat White: Espresso with velvety steamed milk. Similar to a latte but with more espresso.
- Mocha: Espresso with steamed milk and chocolate. Sweet and rich.

TEA
- Americano: Hot water with brewed tea. Available in black, green, or herbal.
- London Fog: Earl Grey tea with steamed milk and vanilla.

PASTRIES
- Croissant: Buttery and flaky. Pairs well with any coffee.
- Blueberry Muffin: Fresh and moist.
- Chocolate Chip Cookie: Classic homemade recipe.

PRICES
- Single Espresso Shot: $2.00
- Double Espresso Shot: $3.00
- Americano: $3.50 (regular), $4.00 (large)
- Latte: $4.50 (regular), $5.00 (large)
- Cappuccino: $4.50 (regular), $5.00 (large)
- Pastries: $3.00 - $5.00
```

### Update application.properties

Add to `src/main/resources/application.properties`:

```properties
# Easy RAG — directory inside src/main/resources; path-type=CLASSPATH required
quarkus.langchain4j.easy-rag.path=rag-docs
quarkus.langchain4j.easy-rag.path-type=CLASSPATH
```

### Test the RAG

Restart `quarkus dev` and try asking:
- "What's on the menu?"
- "How much does a latte cost?"
- "What pastries do you have?"

The barista now answers from the menu document!

---

## Step 7 — Bonus: Conversation Memory

Add persistent conversation memory so the barista remembers context within a session.

### Update BaristaAiService

Modify `src/main/java/org/coffee/BaristaAiService.java`:

```java
package org.coffee;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
@SystemMessage("""
    You are a friendly and knowledgeable barista at The Quarkus Cafe.
    Answer questions about coffee, our menu, and brewing methods.
    Keep responses concise — 2-3 sentences maximum.
    If asked about something unrelated to coffee, politely redirect the conversation.
    """)
public interface BaristaAiService {

    String chat(@MemoryId String memoryId, @UserMessage String message);
}
```

### Update ChatUiResource

Modify `src/main/java/org/coffee/ChatUiResource.java` to generate a session ID:

```java
package org.coffee;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;
import java.util.UUID;

@Path("/chat")
public class ChatUiResource {

    @Inject
    Template chat;

    @Inject
    BaristaAiService baristaAi;

    private final String sessionId = UUID.randomUUID().toString();

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return chat.instance();
    }

    @POST
    @Path("/ask")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String ask(String message) {
        return baristaAi.chat(sessionId, message);
    }
}
```

### Update the Qute template

Add session ID display. In `src/main/resources/templates/chat.html`, update the header:

```html
<div class="header">
    <h1>☕ Quarkus Cafe</h1>
    <p>Ask me anything about coffee! (Session: remembers context)</p>
</div>
```

### Test the memory

Restart `quarkus dev` and try:

1. "I like espresso"
2. "Can you recommend a drink with that?"
3. "Barista should respond: "I'd recommend a cappuccino! Since you like espresso, a cappuccino gives you..."

The barista now remembers that you like espresso and offers contextual recommendations!

---

## Summary

You've built a multi-provider AI chatbot that demonstrates:

- **LangChain4j abstraction:** Same BaristaAiService interface across all providers
- **Provider flexibility:** Swap OpenAI ↔ LlamaCloud ↔ Watson X by changing only pom.xml + application.properties
- **RAG power:** Ground answers in your menu document, eliminating hallucinations
- **Conversational memory:** SessionId-based conversation context
- **Native compilation ready:** All extensions support Quarkus native builds

### Try This Next

- **Switch providers:** Change the extension and configuration, restart, no code changes!
- **Add more documents:** Drop files into `rag-docs/` for RAG
- **Customize the system prompt:** Modify the @SystemMessage to change the barista's personality
- **Deploy native:** `quarkus build -Pnative` for ultra-fast startup and low memory

