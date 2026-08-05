# Wrap-Up: Native Images & Next Steps

**Duration:** 2 minutes &nbsp;|&nbsp; **Instructor demo**

---

## Native Image Demo

!!! warning "Don't run this during the workshop"
    Native compilation takes **3–5 minutes** and requires a builder container. The instructor has a pre-built binary ready to demo. The command below is shown for reference.

    To build your own native image after the workshop:
    ```bash
    cd labs/lab1-rest/solution
    quarkus build --native -Dquarkus.native.container-build=true
    ```
    The `-Dquarkus.native.container-build=true` flag uses a GraalVM builder container — **no local GraalVM installation needed**.

### JVM vs Native: Side-by-Side

The instructor will open two terminal tabs and start the same `menu-service` application in both modes:

**JVM mode:**

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

```
INFO  [io.quarkus] menu-service started in 0.823s.
```

**Native mode:**

```bash
./target/menu-service-1.0.0-SNAPSHOT-runner
```

```
INFO  [io.quarkus] menu-service started in 0.019s.
```

| Metric | JVM | Native |
|--------|-----|--------|
| Startup time | ~800ms | ~20ms |
| Memory (RSS) | ~180 MB | ~35 MB |
| Binary size | ~18 MB JAR + JVM | ~45 MB self-contained |
| Build time | ~5 seconds | ~3–5 minutes |

!!! note "When should you use native?"
    - **Serverless / FaaS** — cold starts must be under 100ms
    - **Kubernetes sidecars** — low memory footprint matters
    - **High-density deployments** — run more instances on the same hardware
    - **General microservices** — JVM mode is fine; native is an optimisation

    The JVM mode is fully production-ready. Native is an option, not a requirement.

---

## What You Built Today

In 60 minutes, starting from nothing, you built:

```
┌──────────────────────────────────────────────────────────────────┐
│                     The Quarkus Cafe System                      │
│                                                                  │
│  ┌────────────────────┐  Kafka "coffee-orders"  ┌─────────────┐  │
│  │   order-service    │ ──────────────────────▶ │menu-service │  │
│  │  POST /orders      │                         │GET  /menu   │  │
│  └────────────────────┘                         │POST /menu 🔒│  │
│                                                 │GET  /menu/  │  │
│  ┌────────────────────┐                         │     info    │  │
│  │   barista-bot      │◀── MCP ──┐              │GET  /menu/  │  │
│  │  GET /chat?msg 🤖  │          │              │   {id}/price│  │
│  └────────────────────┘  ┌───────┴────────┐     └─────────────┘  │
│                          │menu-mcp-server │                      │
│                          │  @Tool methods │                      │
│                          └────────────────┘                      │
└──────────────────────────────────────────────────────────────────┘
```

| Lab | What you learned |
|-----|-----------------|
| Lab 1 | REST API, live reload, continuous testing, Swagger UI |
| Lab 2 | Panache ORM, zero-boilerplate JPA, H2, Dev UI database browser |
| Lab 3 | Config profiles, `@ConfigProperty`, health checks, Dev UI tour |
| Lab 4 | Kafka messaging, DevServices (zero infrastructure config) |
| Lab 5 | OIDC security, Keycloak DevServices, `@Authenticated`, `@RolesAllowed` |
| Lab 6 | Fault tolerance: `@Retry`, `@Fallback`, `@Timeout` |
| Lab 7 | AI chatbot with `@RegisterAiService`, Easy RAG |
| Lab 8 | MCP server with `@Tool`, AI tool calling via `@McpToolBox` |

---

## Next Steps

### Continue Learning

| Resource | Description |
|----------|-------------|
| [quarkus.io/guides](https://quarkus.io/guides) | Comprehensive guides for every extension |
| [code.quarkus.io](https://code.quarkus.io) | Visual project generator — explore all 400+ extensions |
| [quarkus.io/quarkus-workshop-langchain4j](https://quarkus.io/quarkus-workshop-langchain4j) | Official LangChain4j workshop — RAG, tools, guardrails, MCP |
| [quarkiverse.io](https://quarkiverse.io) | Community extensions hub |
| [quarkus.io/blog](https://quarkus.io/blog) | Release notes, tutorials, tips |

### Take the AI Workshop Further

The official [Quarkus LangChain4j Workshop](https://quarkus.io/quarkus-workshop-langchain4j) covers:

- Streaming responses
- Conversation memory (`@MemoryId`)
- Structured outputs (return POJOs from AI methods)
- Guardrails (content safety)
- Advanced RAG (custom retrieval pipeline)
- AI Tools / Function calling
- Model Context Protocol (MCP)
- Agent-to-Agent (A2A) communication

### IBM Enterprise Build of Quarkus

!!! tip "Running Quarkus in production with IBM support?"
    IBM provides an enterprise-supported build of Quarkus — fully compatible with community Quarkus projects, with IBM backing for production deployments.

    Migration from community Quarkus is a two-step `pom.xml` change. Ask your instructor for details or refer to the IBM documentation.

---

## Thank You

You've gone from zero to a full cloud-native microservices system with messaging, security, resilience, and AI — in 60 minutes.

Questions? Comments? The Quarkus community is active at:

- **GitHub:** [github.com/quarkusio/quarkus](https://github.com/quarkusio/quarkus)
- **Zulip chat:** [quarkusio.zulipchat.com](https://quarkusio.zulipchat.com)
- **Stack Overflow:** tag `quarkus`

---

[← Lab 8: MCP Server](lab8-mcp-server.md){ .md-button }
[↑ Back to Home](index.md){ .md-button .md-button--primary }
