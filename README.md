# Quarkus Workshop: From Zero to AI-Powered Microservices

A 60-minute hands-on workshop for Java developers — no prior Quarkus experience needed.

## 📖 Workshop Site

**[View the workshop at https://learn-quarkus.github.io/quarkus-beginners-lab/](https://learn-quarkus.github.io/quarkus-beginners-lab/)**

> Update the URL above after enabling GitHub Pages in your repository settings.

## What You'll Build

A progressive **Coffee Shop** application across 10 labs:

| Lab | Topic | What you build |
|-----|-------|----------------|
| 1 | First REST API | `menu-service` — list and add coffee items |
| 2 | Panache ORM | Persist menu items to H2 with zero boilerplate |
| 3 | Config & Health | Config profiles, health checks, Dev UI tour |
| 4 | Kafka Messaging | `order-service` publishes orders; `menu-service` consumes them |
| 5 | OIDC Security | Protect endpoints with Keycloak — zero manual setup |
| 6 | Fault Tolerance | `@Retry`, `@Fallback`, `@Timeout` in 3 annotations |
| 7 | AI with LangChain4j | `barista-bot` — an AI assistant powered by OpenAI + RAG |
| 8 | MCP Server | Expose menu data as MCP tools; wire them into the barista bot |
| 9 | Containerization | Build a container image with Jib; generate Kubernetes manifests |
| 10 *(optional)* | Quarkus Flow | Agentic order workflow with configurable HITL approval |

## Prerequisites

- Java 21+
- Maven 3.9+ or Quarkus CLI
- Docker Desktop or Podman (Labs 4, 5 & 9)
- OpenAI API key (Labs 7, 8 & 10)

See the [Prerequisites page](https://learn-quarkus.github.io/quarkus-beginners-lab/00-prerequisites/) for full setup instructions.

## Repository Structure

```
quarkus-workshop/
├── docs/          # MkDocs source — the workshop website
├── labs/          # Runnable solution code per lab
├── mkdocs.yml     # Site configuration
├── prereq-check.sh
└── .github/workflows/deploy.yml
```

## Running the Site Locally

```bash
pip install mkdocs-material
mkdocs serve
# Open http://localhost:8000
```

## License

Apache License 2.0

## Acknowledgements

This workshop was built with the assistance of <a href="https://www.ibm.com/products/ibm-bob" target="_blank">IBM Bob</a>, an AI software engineering assistant by IBM.
