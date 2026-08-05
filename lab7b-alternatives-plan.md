# Lab 7B: Alternative LLM Providers — Plan

## Overview

Create an optional Lab 7B documentation and solution code that teaches learners how to build the same barista-bot chatbot using three LLM provider options: OpenAI, LlamaCloud (free tier), and Watson X AI. The documentation will use MkDocs tabs to present all three as equal choices, include links to free tier signups, and provide working solution code for each provider.

**Key principle:** The BaristaAiService interface code remains identical across all three providers—only dependencies and configuration differ. This demonstrates LangChain4j's abstraction power.

**Deliverables:**
1. New documentation file: `docs/lab7b-langchain4j-alternatives.md`
2. Three solution directories in `labs/lab7-langchain4j/`:
   - `solution-openai/` (reference implementation)
   - `solution-llamacloud/` (free tier)
   - `solution-watsonx/` (IBM Watson X AI)
3. Links to free tier signup pages embedded in documentation

---

## Sub-Tasks

### Sub-Task 1: Research and Validate Provider Integrations
**Intent:** Confirm which LangChain4j Quarkus extensions support LlamaCloud and Watson X, identify the correct dependency names, and document configuration properties and API key requirements.

**Expected Outcomes:**
- Confirmed extension names and Maven coordinates for all three providers
- Documented configuration properties for each provider's `application.properties`
- Identified environment variable names for API keys (e.g., `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY`)
- Links to official Quarkus LangChain4j documentation for each provider
- Free tier signup/trial URLs for LlamaCloud and Watson X

**Todo List:**
1. [ ] Search Quarkus LangChain4j documentation for supported providers
2. [ ] Document extension name, group ID, artifact ID for OpenAI integration
3. [ ] Document extension name, group ID, artifact ID for LlamaCloud (Ollama) integration
4. [ ] Document extension name, group ID, artifact ID for Watson X integration
5. [ ] List required `application.properties` configuration keys for each provider
6. [ ] Identify API key environment variables for each provider
7. [ ] Collect official signup/trial URLs:
   - LlamaCloud free tier link
   - Watson X free trial link
   - OpenAI API key setup link
8. [ ] Document any pricing, rate limits, or free tier restrictions
9. [ ] Verify that BaristaAiService interface code is provider-agnostic (no changes needed)

**Relevant Context:**
- Current Lab 7 uses `quarkus-langchain4j-openai` (group: `io.quarkiverse.langchain4j`)
- LangChain4j BOM is imported in pom.xml
- Configuration pattern: `quarkus.langchain4j.<provider>.*`
- Reference: [`labs/lab7-langchain4j/solution/pom.xml`](labs/lab7-langchain4j/solution/pom.xml)
- Reference: [`labs/lab7-langchain4j/solution/src/main/resources/application.properties`](labs/lab7-langchain4j/solution/src/main/resources/application.properties)

**Status:** `[ ] pending`

---

### Sub-Task 2: Create Lab 7B Documentation
**Intent:** Write comprehensive, learner-friendly documentation that presents all three provider options as tabs at each relevant step, includes free tier signup links, and clearly explains prerequisites and configuration differences.

**Expected Outcomes:**
- Completed `docs/lab7b-langchain4j-alternatives.md` file
- All code snippets (maven commands, pom.xml, properties) use tab syntax with all 3 providers
- Free tier signup/trial links prominently placed in prerequisites section
- Clear guidance on API key setup for each provider
- Consistent with existing Lab 7 documentation style and structure
- Bonus steps (RAG, Memory) included with provider-agnostic examples

**Todo List:**
1. [ ] Create new file `docs/lab7b-langchain4j-alternatives.md`
2. [ ] Write header/overview section explaining the lab purpose
3. [ ] Add prerequisites section with:
   - OpenAI API key setup link
   - LlamaCloud free tier signup link + brief setup instructions
   - Watson X free trial signup link + brief setup instructions
4. [ ] Add Step 1: Project creation (copy from Lab 7, no tabs needed)
5. [ ] Add Step 2: Add dependencies with tabs for all 3 providers
   - Quarkus CLI command
   - Maven command
6. [ ] Add Step 3: Configure application.properties with tabs for all 3 providers
   - Show `quarkus.langchain4j.<provider>.*` properties
   - Show environment variable names for API keys
7. [ ] Add Step 4: Define BaristaAiService interface (identical code, no tabs)
8. [ ] Add Step 5: Add Chat UI (copy from Lab 7, no tabs needed)
9. [ ] Add Step 6: Bonus RAG (provider-agnostic, copy from Lab 7)
10. [ ] Add Step 7: Bonus Memory (provider-agnostic, copy from Lab 7)
11. [ ] Add comparison table contrasting the three providers (features, pricing, free limits)
12. [ ] Add Summary section

**Relevant Context:**
- Existing Lab 7 structure: [`docs/lab7-langchain4j.md`](docs/lab7-langchain4j.md)
- MkDocs tab syntax: `=== "Tab Name"`
- Tabs already used in Lab 7 for OS-specific commands (macOS/Linux vs Windows)
- File location: `docs/` directory
- Will be linked from main index or lab navigation

**Status:** `[ ] pending`

---

### Sub-Task 3: Create Solution Code for LlamaCloud
**Intent:** Build a working barista-bot solution using LlamaCloud's free tier, demonstrating provider-specific setup (dependencies and configuration only—no code changes).

**Expected Outcomes:**
- Complete `labs/lab7-langchain4j/solution-llamacloud/` directory with:
  - pom.xml with `quarkus-langchain4j-ollama` dependency
  - src/main/resources/application.properties with LlamaCloud-specific config
  - Identical BaristaAiService.java from Lab 7 (no changes)
  - Identical ChatUiResource.java from Lab 7 (no changes)
  - Identical Qute template from Lab 7 (no changes)
- Verified to compile and run with LlamaCloud backend

**Todo List:**
1. [ ] Copy Lab 7 solution directory structure to `labs/lab7-langchain4j/solution-llamacloud/`
2. [ ] Update pom.xml: replace `quarkus-langchain4j-openai` with `quarkus-langchain4j-ollama`
3. [ ] Update application.properties with LlamaCloud configuration:
   - Set model name for Ollama (e.g., `llama2`, `neural-chat`)
   - Keep logging enabled for learning purposes
4. [ ] Copy BaristaAiService.java unchanged
5. [ ] Copy ChatUiResource.java unchanged
6. [ ] Copy Qute template and other resources unchanged
7. [ ] Verify pom.xml builds without errors
8. [ ] Create README or comments explaining provider-specific differences

**Relevant Context:**
- Base Lab 7 solution: [`labs/lab7-langchain4j/solution/`](labs/lab7-langchain4j/solution/)
- LlamaCloud uses Ollama backend in LangChain4j
- Configuration property prefix: `quarkus.langchain4j.ollama.*`
- API key environment variable: `QUARKUS_LANGCHAIN4J_OLLAMA_API_KEY`

**Status:** `[ ] pending`

---

### Sub-Task 4: Create Solution Code for Watson X AI
**Intent:** Build a working barista-bot solution using Watson X AI, demonstrating provider-specific setup (dependencies and configuration only—no code changes).

**Expected Outcomes:**
- Complete `labs/lab7-langchain4j/solution-watsonx/` directory with:
  - pom.xml with `quarkus-langchain4j-watsonx` dependency
  - src/main/resources/application.properties with Watson X-specific config
  - Identical BaristaAiService.java from Lab 7 (no changes)
  - Identical ChatUiResource.java from Lab 7 (no changes)
  - Identical Qute template from Lab 7 (no changes)
- Verified to compile and run with Watson X backend

**Todo List:**
1. [ ] Copy Lab 7 solution directory structure to `labs/lab7-langchain4j/solution-watsonx/`
2. [ ] Update pom.xml: replace `quarkus-langchain4j-openai` with `quarkus-langchain4j-watsonx`
3. [ ] Update application.properties with Watson X configuration:
   - Set Watson X API endpoint URL
   - Set model name (e.g., `meta-llama/llama-2-70b-chat`)
   - Keep logging enabled for learning purposes
4. [ ] Copy BaristaAiService.java unchanged
5. [ ] Copy ChatUiResource.java unchanged
6. [ ] Copy Qute template and other resources unchanged
7. [ ] Verify pom.xml builds without errors
8. [ ] Create README or comments explaining provider-specific differences

**Relevant Context:**
- Base Lab 7 solution: [`labs/lab7-langchain4j/solution/`](labs/lab7-langchain4j/solution/)
- Configuration property prefix: `quarkus.langchain4j.watsonx.*`
- May require API key and project/instance ID
- Environment variables: `QUARKUS_LANGCHAIN4J_WATSONX_API_KEY`, etc.

**Status:** `[ ] pending`

---

### Sub-Task 5: Create Solution Code for OpenAI (Reference)
**Intent:** Reorganize existing Lab 7 solution as `solution-openai/` reference implementation to maintain consistency with LlamaCloud and Watson X solutions.

**Expected Outcomes:**
- Existing `labs/lab7-langchain4j/solution/` renamed/reorganized as `labs/lab7-langchain4j/solution-openai/`
- OR new `labs/lab7-langchain4j/solution-openai/` directory created with same content
- Maintains backward compatibility with existing documentation

**Todo List:**
1. [ ] Decide: rename existing solution or create new copy?
   - Recommend: create `solution-openai/` as a copy, keep original `solution/` for backward compatibility
2. [ ] Copy Lab 7 solution to `labs/lab7-langchain4j/solution-openai/`
3. [ ] Verify all files are intact
4. [ ] Add comment in solution/ README explaining the new alternative solutions exist

**Relevant Context:**
- Existing solution: [`labs/lab7-langchain4j/solution/`](labs/lab7-langchain4j/solution/)
- Will be parallel to `solution-llamacloud/` and `solution-watsonx/`

**Status:** `[ ] pending`

---

## Implementation Order

Execute sub-tasks in this sequence:

1. **Sub-Task 1** (Research) — Must complete first to gather technical requirements
2. **Sub-Task 2** (Documentation) — Can proceed in parallel with Sub-Task 3-5 once Sub-Task 1 is done
3. **Sub-Task 3** (LlamaCloud) — Use findings from Sub-Task 1
4. **Sub-Task 4** (Watson X) — Use findings from Sub-Task 1
5. **Sub-Task 5** (OpenAI Reference) — Final step to organize existing code

---

## Success Criteria

- [ ] Documentation clearly explains all 3 provider options as equal choices
- [ ] All code snippets use MkDocs tab syntax for provider selection
- [ ] Free tier signup/trial links are prominently featured in prerequisites
- [ ] Solution code for each provider compiles and deploys without errors
- [ ] BaristaAiService interface is identical across all three solutions (proving LangChain4j abstraction)
- [ ] Lab 7B can be completed by learners following tabs without confusion
- [ ] Documentation is consistent with existing Lab 7 style and structure
