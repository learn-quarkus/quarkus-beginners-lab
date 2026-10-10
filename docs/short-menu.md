# Quarkus Beginners Lab — Short Menu

---

## Core Modules

| Lab | Topic | Prerequisites |
|-----|-------|--------------|
| [Lab 1](short-lab1-rest.md) | **First REST API** — Dev Mode, Live Coding, Continuous Testing | None |
| [Lab 2](short-lab2-kafka.md) | **Kafka Messaging with DevServices** | Lab 1 · 🐳 Docker or Podman |
| [Lab 3](short-lab3-langchain4j.md) | **LangChain4j AI Chatbot** | Lab 1 · 🔑 OpenAI key |
| [Lab 4](short-lab4-mcp-server.md) | **MCP Server** — Expose menu as AI tools | Lab 3 · 🔑 OpenAI key |

---

## Story Progression

**Lab 1 → Lab 2 → Lab 3 → Lab 4** builds incrementally:

- **Lab 1 — Build the API:** Create a REST API that exposes the Quarkus Cafe menu and persists data to a database. You have a functional backend.
- **Lab 2 — Add Events:** Wire Kafka so order submissions flow as decoupled events. The system now scales and stays responsive under load.
- **Lab 3 — Add Intelligence:** Build `barista-bot`, an AI assistant that answers customer questions about the menu. It grounds answers in your actual menu document, avoiding hallucination.
- **Lab 4 — Live Tool Calling:** Expose the menu as MCP tools the AI can invoke directly. The bot calls methods in real time instead of relying on injected text — always fresh, always accurate.

**End result:** A complete microservice ecosystem where customers ask an AI assistant about your menu, which calls your live backend for exact, up-to-date answers.

---

## What You'll Build

- **Lab 1**: REST API for the Quarkus Cafe menu
- **Lab 2**: Order events published to Kafka
- **Lab 3**: AI chatbot that answers menu questions
- **Lab 4**: LLM-callable tools via Model Context Protocol

---

## Setup

Before starting, complete the [Prerequisites](00-prerequisites.md):

- Java 21+
- Maven 3.9+ or Quarkus CLI
- Docker Desktop or Podman Desktop (for Lab 4)
- OpenAI API key (for Labs 7 & 8)

[→ Start with Lab 1](short-lab1-rest.md){ .md-button .md-button--primary }
