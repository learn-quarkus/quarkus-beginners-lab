# Quarkus Workshop: From Zero to AI-Powered Microservices

A 60-minute hands-on workshop for Java developers — no prior Quarkus experience needed.

## 📖 Workshop Site

**[View the workshop at https://learn-quarkus.github.io/quarkus-beginners-lab/](https://learn-quarkus.github.io/quarkus-beginners-lab/)**

> Update the URL above after enabling GitHub Pages in your repository settings.

## What You'll Build

A progressive **Coffee Shop** application across 7 labs:

| Lab | Topic | What you build |
|-----|-------|----------------|
| 1 | First REST API | `menu-service` — list and add coffee items |
| 2 | Panache ORM | Persist menu items to H2 with zero boilerplate |
| 3 | Config & Health | Config profiles, health checks, Dev UI tour |
| 4 | Kafka Messaging | `order-service` publishes orders; `menu-service` consumes them |
| 5 | OIDC Security | Protect endpoints with Keycloak — zero manual setup |
| 6 | Fault Tolerance | `@Retry`, `@Fallback`, `@Timeout` in 3 annotations |
| 7 | AI with LangChain4j | `barista-bot` — an AI assistant powered by OpenAI + RAG |

## Prerequisites

- Java 21+
- Maven 3.9+ or Quarkus CLI
- Docker Desktop or Podman (Labs 4 & 5 only)
- OpenAI API key (Lab 7 only)

See the [Prerequisites page](https://your-org.github.io/quarkus-workshop/00-prerequisites/) for full setup instructions.

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
