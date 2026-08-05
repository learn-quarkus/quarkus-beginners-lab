# barista-bot — OpenAI Provider

This is the solution for Lab 7B using **OpenAI GPT-4o-mini** as the LLM provider.

## Setup

Before running:

```bash
export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-...
quarkus dev
```

## Key Configuration

- **Dependency:** `quarkus-langchain4j-openai`
- **Model:** `gpt-4o-mini` (fast and cost-effective)
- **API Key env var:** `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY`
- **Config file:** `src/main/resources/application.properties`

## Code Note

The `BaristaAiService.java` interface is **identical** across all three provider solutions (OpenAI, LlamaCloud, Watson X).
Only the `pom.xml` and `application.properties` differ—this demonstrates LangChain4j's abstraction power.

## Run

```bash
quarkus dev
```

Open [http://localhost:8080/chat](http://localhost:8080/chat)
