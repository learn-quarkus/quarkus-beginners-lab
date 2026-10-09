# Quarkus Beginners Lab — Short Menu

Quick-start path: **Labs 1 → 4 → 7 → 8**

---

## Core Modules

| Lab | Topic | Prerequisites |
|-----|-------|--------------|
| [Lab 1](lab1-rest.md) | **First REST API** — Dev Mode, Live Coding, Continuous Testing | None |
| [Lab 4](lab4-kafka.md) | **Kafka Messaging with DevServices** | Lab 1 · 🐳 Docker or Podman |
| [Lab 7](lab7-langchain4j.md) | **LangChain4j AI Chatbot** | Lab 1 · 🔑 OpenAI key |
| [Lab 8](lab8-mcp-server.md) | **MCP Server** — Expose menu as AI tools | Lab 7 · 🔑 OpenAI key |

---

## Story Progression

**Lab 1 → Lab 4 → Lab 7 → Lab 8** builds incrementally:

- **Lab 1 — Build the API:** Create a REST API that exposes the Quarkus Cafe menu and persists data to a database. You have a functional backend.
- **Lab 4 — Add Events:** Wire Kafka so order submissions flow as decoupled events. The system now scales and stays responsive under load.
- **Lab 7 — Add Intelligence:** Build `barista-bot`, an AI assistant that answers customer questions about the menu. It grounds answers in your actual menu document, avoiding hallucination.
- **Lab 8 — Live Tool Calling:** Expose the menu as MCP tools the AI can invoke directly. The bot calls methods in real time instead of relying on injected text — always fresh, always accurate.

**End result:** A complete microservice ecosystem where customers ask an AI assistant about your menu, which calls your live backend for exact, up-to-date answers.

---

## What You'll Build

- **Lab 1**: REST API for the Quarkus Cafe menu
- **Lab 4**: Order events published to Kafka
- **Lab 7**: AI chatbot that answers menu questions
- **Lab 8**: LLM-callable tools via Model Context Protocol

---

## Setup

Before starting, complete the [Prerequisites](00-prerequisites.md):
- Java 21+
- Maven 3.9+ or Quarkus CLI
- Docker Desktop or Podman Desktop (for Lab 4)
- OpenAI API key (for Labs 7 & 8)

[→ Start with Lab 1](lab1-rest.md){ .md-button .md-button--primary }
