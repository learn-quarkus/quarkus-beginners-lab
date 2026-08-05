# barista-bot — IBM Watson X AI Provider

This is the solution for Lab 7B using **IBM Watson X AI** as the LLM provider.

## Setup

Before running:

1. Sign up for free trial: [ibm.com/cloud/watsonx/ai](https://www.ibm.com/cloud/watsonx/ai)
2. Create IBM Cloud account and Watson project
3. Generate API key in Watson Studio
4. Get your Project ID (or Space ID)
5. Set the environment variables:

```bash
export QUARKUS_LANGCHAIN4J_WATSONX_API_KEY=your-api-key
export QUARKUS_LANGCHAIN4J_WATSONX_PROJECT_ID=your-project-id
export QUARKUS_LANGCHAIN4J_WATSONX_BASE_URL=https://us-south.ml.cloud.ibm.com
quarkus dev
```

## Key Configuration

- **Dependency:** `quarkus-langchain4j-watsonx`
- **Model:** `meta-llama/llama-2-70b-chat` (or choose from Watson model catalog)
- **API endpoints:** Multiple regions available
- **Required API credentials:**
  - `QUARKUS_LANGCHAIN4J_WATSONX_API_KEY` — IBM Cloud API key
  - `QUARKUS_LANGCHAIN4J_WATSONX_PROJECT_ID` — Watson project/space ID
  - `QUARKUS_LANGCHAIN4J_WATSONX_BASE_URL` — Regional endpoint (e.g., us-south, eu-de)
- **Config file:** `src/main/resources/application.properties`

## Available Models

Watson X offers a wide range of models:
- IBM Granite models (proprietary, fine-tuned for enterprise)
- Meta Llama 2 (70B chat, multi-language)
- Mistral (fast, lightweight)
- And many more from the model catalog

## Code Note

The `BaristaAiService.java` interface is **identical** across all three provider solutions (OpenAI, LlamaCloud, Watson X).
Only the `pom.xml` and `application.properties` differ—this demonstrates LangChain4j's abstraction power.

## Run

```bash
quarkus dev
```

Open [http://localhost:8080/chat](http://localhost:8080/chat)

## Switching to OpenAI or LlamaCloud

To try a different provider without touching Java code:
1. Update `pom.xml`: replace `quarkus-langchain4j-watsonx` with `quarkus-langchain4j-openai` or `quarkus-langchain4j-ollama`
2. Update `application.properties` with the provider's config
3. Update environment variables for API keys
4. Restart `quarkus dev`
5. No Java changes needed!
