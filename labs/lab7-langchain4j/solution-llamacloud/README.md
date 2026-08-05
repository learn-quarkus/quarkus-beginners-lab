# barista-bot — LlamaCloud Free Tier Provider

This is the solution for Lab 7B using **LlamaCloud free tier** as the LLM provider.

## Setup

Before running:

1. Sign up for free tier: [llamaindex](https://login.llamaindex.ai/)
2. Generate an API key
3. Set the environment variable:

```bash
export QUARKUS_LANGCHAIN4J_OLLAMA_API_KEY=your-api-key
quarkus dev
```

## Key Configuration

- **Dependency:** `quarkus-langchain4j-ollama` (LlamaCloud uses Ollama-compatible API)
- **Model:** `llama2` (open-source, excellent quality)
- **Base URL:** `https://api.llamaindex.ai/ollama`
- **API Key env var:** `QUARKUS_LANGCHAIN4J_OLLAMA_API_KEY`
- **Config file:** `src/main/resources/application.properties`

## Code Note

The `BaristaAiService.java` interface is **identical** across all three provider solutions (OpenAI, LlamaCloud, Watson X).
Only the `pom.xml` and `application.properties` differ—this demonstrates LangChain4j's abstraction power.

## Run

```bash
quarkus dev
```

Open [http://localhost:8080/chat](http://localhost:8080/chat)

## Switching to OpenAI or Watson X

To try a different provider without touching Java code:
1. Update `pom.xml`: replace `quarkus-langchain4j-ollama` with `quarkus-langchain4j-openai` or `quarkus-langchain4j-watsonx`
2. Update `application.properties` with the provider's config
3. Update environment variables for API keys
4. Restart `quarkus dev`
5. No Java changes needed!
