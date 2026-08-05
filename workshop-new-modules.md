# Quarkus Workshop — New Module Ideas

Research-backed suggestions drawn from official framework workshops (Quarkus Super Heroes, Quarkus LangChain4j, Quarkus Flow, Micronaut guides, Helidon workshop, Spring Boot microservices courses).

---

## Tier 1 — Highest impact, laptop-safe, no new prerequisites

### Lab 9 option A — Observability: Metrics, Traces & Grafana (DevServices)

**Type:** New standalone lab  
**Duration:** ~8 min  
**Project:** `menu-service` (continued)  
**Extensions:** `quarkus-micrometer-registry-prometheus`, `quarkus-opentelemetry`, `quarkus-observability-devservices-lgtm`  
**Docker:** Required (already needed from Lab 4)

Add `quarkus-micrometer-registry-prometheus` and `quarkus-opentelemetry` to `menu-service`. Quarkus automatically instruments every HTTP request, Kafka message, and database call with no code changes. Add `quarkus-observability-devservices-lgtm` — a single Docker image that auto-starts Grafana, Prometheus, Tempo (traces), and Loki (logs). Students open Grafana at `localhost:3000` and see live dashboards with zero configuration.

Add one `@WithSpan` annotation to `PricingService.getPrice()` to show a custom trace span. Students see the Lab 6 retry storms as spiky trace histograms in Grafana.

Coffee-shop angle: *"How many espressos were ordered in the last 5 minutes? Why is `GET /menu` slow today?"*

**Inspired by:** Quarkus LangChain4j Workshop Step 10 (observability + fault tolerance), Quarkus Super Heroes workshop (statistics microservice), official Quarkus LGTM DevServices guide. Spring Boot workshops cover the same story with Actuator + Zipkin — the Quarkus version requires zero manual setup.

---

### Lab 9 option B — Real-Time Orders: WebSockets with quarkus-websockets-next

**Type:** New standalone lab  
**Duration:** ~8 min  
**Project:** `menu-service` + `order-service` (from Lab 4)  
**Extensions:** `quarkus-websockets-next`  
**Docker:** Not required

Add `quarkus-websockets-next` to `menu-service`. Create a `@WebSocket(path = "/orders/live")` endpoint. When `OrderConsumer` receives a Kafka message (Lab 4), broadcast it to all connected WebSocket clients instead of just logging it. Students open a browser tab showing a live order ticker — no polling, no page refresh.

Coffee-shop angle: a barista's screen that shows new orders in real time as customers place them from `order-service`.

API:
- `@OnOpen` — send current queue summary to newly connected client
- `@OnTextMessage` — echo commands (e.g. "mark order complete")
- Kafka `@Incoming` consumer calls `WebSocketConnection.broadcast()` on each new order

**Inspired by:** Quarkus LangChain4j Workshop Step 1 uses `@WebSocket` + `@OnTextMessage` for the AI chatbot. Quarkus Super Heroes workshop builds a statistics WebSocket that broadcasts fight results live. Spring Boot workshops universally include a WebSocket chat or live-feed lab as the "real-time" showcase.

---

### Lab 9 option C — Caching with Redis DevServices (`@CacheResult`)

**Type:** New standalone lab  
**Duration:** ~6 min  
**Project:** `menu-service` (continued from Lab 6)  
**Extensions:** `quarkus-redis-cache`  
**Docker:** Required (DevServices starts Redis automatically)

Add `quarkus-redis-cache` to `menu-service`. Annotate `PricingService.getPrice()` from Lab 6 with `@CacheResult(cacheName = "prices")`. The first call hits the (simulated) pricing backend; subsequent calls for the same item ID return the cached value instantly from Redis. Add `@CacheInvalidate` on a new `DELETE /menu/{id}` endpoint to show cache invalidation.

Redis starts automatically via DevServices — same zero-config pattern as Kafka (Lab 4) and Keycloak (Lab 5).

Key annotations: `@CacheResult`, `@CacheInvalidate`, `@CacheKey`

**Inspired by:** Micronaut's caching guide is one of its most-visited sections and prominently features Redis. Spring Boot workshops always include a Redis caching lab. The Quarkus Redis API blog post was a top-featured Quarkus post. Official guide: https://quarkus.io/guides/cache-redis-reference

---

## Tier 2 — Strong content, slightly more scope

### Lab 9 option D — REST Client: `@RegisterRestClient` + WireMock DevServices

**Type:** New standalone lab  
**Duration:** ~8 min  
**Project:** `menu-service` (continued from Lab 6)  
**Extensions:** `quarkus-rest-client-jackson`, `quarkus-wiremock` (test scope)  
**Docker:** Not required

Replace the random-failure simulation in `PricingService` (Lab 6) with a real outgoing HTTP call using a `@RegisterRestClient` interface. Add `quarkus-wiremock` as a DevService that auto-starts a WireMock stub server in dev mode, serving canned pricing responses. The `@Retry` / `@Fallback` from Lab 6 wrap the REST client call exactly as before — students see how fault tolerance applies to real HTTP calls.

`@RegisterRestClient` declares a type-safe HTTP client as a Java interface — same philosophy as `@RegisterAiService` from Lab 7, which makes it immediately familiar to students.

**Inspired by:** Quarkus Super Heroes workshop (Hero and Villain services call each other via `@RegisterRestClient`). Helidon workshop uses MicroProfile REST Client in the same pattern. Every Spring Boot microservices course has a Feign/RestTemplate module.

---

### Bonus step in Lab 3 — Scheduled Jobs: Daily Special (`@Scheduled`)

**Type:** Bonus step (add to end of Lab 3)  
**Duration:** ~4 min  
**Project:** `menu-service` (continued)  
**Extensions:** `quarkus-scheduler`  
**Docker:** Not required

Add a `@Scheduled(every = "30s")` CDI bean that selects a random `MenuItem` from the database and stores it as today's special in a field. Expose it at `GET /menu/special`. Students see the "special" change every 30 seconds without any HTTP request triggering it.

Mention: `@Scheduled(cron = "0 8 * * *")` for production use.

**Inspired by:** Micronaut's scheduling guide is a top-10 visited page. Spring Boot workshops always include a `@Scheduled` lab. The 30-second interval makes the demo instant.

---

### Lab 9 option E — Full-Text Menu Search: Hibernate Search + Elasticsearch DevServices

**Type:** New standalone lab  
**Duration:** ~8 min  
**Project:** `menu-service` (continued)  
**Extensions:** `quarkus-hibernate-search-orm-elasticsearch`  
**Docker:** Required (DevServices starts Elasticsearch automatically)

Add `quarkus-hibernate-search-orm-elasticsearch` to `menu-service`. Annotate `MenuItem.name` and `MenuItem.description` with `@FullTextField` and the class with `@Indexed`. Add a `GET /menu/search?q=oat` endpoint backed by a Hibernate Search query.

Students see `?q=oat` find "Cappuccino" and "Cold Brew" via fuzzy full-text search — not a SQL `LIKE`. Natural complement to the Easy RAG demo from Lab 7: *keyword search vs semantic search*.

**Inspired by:** One of the most-starred Quarkus guides. Common in enterprise Java courses. Official guides: https://quarkus.io/guides/hibernate-search-orm-elasticsearch

---

## Tier 3 — Compelling but heavier; better suited to a half-day or extended workshop

### Lab 10 option A — Agentic AI: barista-bot takes real actions

**Type:** Advanced standalone lab  
**Duration:** ~12 min  
**Project:** `barista-bot` + `order-service`  
**Extensions:** None new (langchain4j already present)  
**Docker:** Not required

Give `barista-bot` `@Tool`-annotated methods that call `order-service` and `menu-service` directly via `@RegisterRestClient`. Ask: *"Book me two espressos."* — the bot calls `POST /orders`, gets the confirmation, and replies: *"Done! Your order is being prepared."* The LLM decides which tools to call; students write only the Java methods.

**Inspired by:** Quarkus Flow Workshop Labs 4–5 (agentic workflows). Quarkus LangChain4j Workshop Steps 4–6 cover tools and function calling extensively.

---

### Bonus step in Lab 7 — AI Guardrails: block off-topic input

**Type:** Bonus step (add to Lab 7)  
**Duration:** ~5 min  
**Project:** `barista-bot`  
**Extensions:** None new  
**Docker:** Not required

Add `@InputGuardrails` annotation to `BaristaAiService.chat()` pointing to a guardrail class that returns a failure if the user's message is not coffee-related. The bot refuses *"Write me a poem about cars"* without hitting the model — saving tokens and enforcing safe use.

**Inspired by:** Quarkus LangChain4j Workshop Step 10 demonstrates `@InputGuardrails` + `@OutputGuardrails` as a production-readiness step.

---

### Lab 10 option B — Kubernetes: auto-generated manifests with health probe wiring

**Type:** Advanced standalone lab  
**Duration:** ~10 min  
**Project:** `menu-service`  
**Extensions:** `quarkus-kubernetes`, `quarkus-container-image-jib`  
**Docker:** Required + kubectl + kind or minikube

Add `quarkus-kubernetes` to `menu-service`. Run `quarkus build` — Quarkus generates a complete `kubernetes.yml` in `target/kubernetes/` with Deployment, Service, and health-check probes auto-wired to `/q/health/live` and `/q/health/ready` (from Lab 3). Deploy to a local cluster. The health checks written in Lab 3 power the Kubernetes probes with no extra configuration.

**Inspired by:** Quarkus Super Heroes workshop deploys all services to Kubernetes. Helidon workshop includes a Kubernetes deployment lab. Spring Boot microservices courses almost universally end with a Kubernetes section.

---

### Bonus step anywhere — Virtual Threads (`@RunOnVirtualThread`)

**Type:** Bonus step  
**Duration:** ~4 min  
**Project:** `menu-service`  
**Extensions:** None (Java 21 already required)  
**Docker:** Not required

Annotate `MenuResource` methods with `@RunOnVirtualThread`. Show a thread dump before and after in the Dev UI: blocking DB calls that previously occupied a platform thread now run on virtual threads. The thread name change in the logs is immediate proof — no benchmark needed.

**Inspired by:** Helidon 4 was built entirely on virtual threads and promotes this as its headline feature. Spring Boot 3.2+ virtual thread support is a major conference topic. Quarkus 3.x supports `@RunOnVirtualThread` natively.

---

## Quick-reference table

| Module | Type | Time | New extensions | Docker? | Inspired by |
|--------|------|------|----------------|---------|-------------|
| Observability — Grafana LGTM | New lab | ~8 min | `micrometer-registry-prometheus`, `opentelemetry`, `observability-devservices-lgtm` | Yes (already needed) | Quarkus LangChain4j WS Step 10, Super Heroes WS |
| WebSockets — live order ticker | New lab | ~8 min | `websockets-next` | No | Quarkus LangChain4j WS Step 1, Super Heroes WS |
| Redis Caching — `@CacheResult` | New lab | ~6 min | `redis-cache` | Yes | Micronaut guides, Spring Boot workshops |
| REST Client + WireMock | New lab | ~8 min | `rest-client-jackson`, `quarkus-wiremock` | No | Quarkus Super Heroes WS, Helidon WS |
| Scheduled Jobs — daily special | Bonus in Lab 3 | ~4 min | `scheduler` | No | Micronaut guides, Spring Boot workshops |
| Full-Text Search — Hibernate Search | New lab | ~8 min | `hibernate-search-orm-elasticsearch` | Yes | Quarkus guides, Spring Data ES courses |
| Agentic AI — barista takes actions | Advanced lab | ~12 min | None | No | Quarkus Flow WS Labs 4–5 |
| AI Guardrails | Bonus in Lab 7 | ~5 min | None | No | Quarkus LangChain4j WS Step 10 |
| Kubernetes — auto-generated manifests | Advanced lab | ~10 min | `kubernetes`, `container-image-jib` | Yes + kubectl | Quarkus Super Heroes WS, Helidon WS |
| Virtual Threads — `@RunOnVirtualThread` | Bonus anywhere | ~4 min | None | No | Helidon 4, Spring Boot 3.2 |
