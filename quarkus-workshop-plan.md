# Quarkus Hands-On Workshop Plan
## "From Zero to AI-Powered Microservices in 60 Minutes"

---

## Overview

**Goal:** Guide Java developers who have never used Quarkus through a progressive, laptop-only workshop covering the full Quarkus developer experience — from bootstrapping a first app all the way to building an AI chatbot with LangChain4j.

**Audience:** Beginner Java developers with no prior Quarkus experience.

**Duration:** 60 minutes total (instructor-led, hands-on).

**Theme:** A **Coffee Shop** domain used consistently across all labs:
- Lab 1–3: `menu-service` — a REST API for coffee menu items
- Lab 4: `order-service` — publishes orders to Kafka, `menu-service` consumes them
- Lab 5: `menu-service` secured — only authenticated baristas can add items
- Lab 6: `menu-service` made resilient with fault tolerance
- Lab 7: `barista-bot` — an AI assistant that answers questions about the menu

**Runtime constraint:** Everything runs on a laptop. No OpenShift. Docker or Podman is required (for DevServices in Labs 4 and 5 only).

---

## Prerequisites (Attendee Setup — Before the Workshop)

- Java 21+
- Maven 3.9+ or Quarkus CLI (`sdk install quarkus` / `brew install quarkus`)
- Docker Desktop or Podman (for Labs 4 & 5 DevServices)
- OpenAI API key (for Lab 7)
- IDE (VS Code + Quarkus extension or IntelliJ recommended)

---

## Workshop Schedule

| Time | Lab | Topic |
|------|-----|-------|
| 00:00 – 05:00 | Intro | Why Quarkus, what we're building |
| 05:00 – 17:00 | Lab 1 | First REST API, Dev Mode, Live Coding, Continuous Testing |
| 17:00 – 25:00 | Lab 2 | Panache ORM + H2 — persist menu items |
| 25:00 – 31:00 | Lab 3 | Config profiles + Health checks + Dev UI tour |
| 31:00 – 38:00 | Lab 4 | Kafka messaging with DevServices |
| 38:00 – 45:00 | Lab 5 | OIDC Security + Keycloak DevServices |
| 45:00 – 50:00 | Lab 6 | Fault Tolerance (@Retry, @Fallback) |
| 50:00 – 58:00 | Lab 7 | LangChain4j AI chatbot + Easy RAG bonus |
| 58:00 – 60:00 | Wrap | Native image demo (pre-built) + Resources |

---

## Sub-Tasks

---

### Sub-Task 0a — GitHub Pages Site Infrastructure

**Intent:** Set up the MkDocs + Material theme static site that will be published via GitHub Pages. This is the attendee-facing website they browse during the workshop. All lab instructions, code snippets, and explanations will live as pages under `docs/`. A GitHub Actions workflow publishes the site automatically on every push to `main`.

**Expected Outcomes:**
- `mkdocs.yml` at the repo root defines the site title, theme, and full navigation tree
- `docs/` directory mirrors all labs as fully navigable pages with a left sidebar
- GitHub Actions `deploy.yml` auto-builds and deploys to `gh-pages` branch on push to `main`
- Site is live at `https://<username>.github.io/<repo>/` with working nav, search, and copy buttons on all code blocks
- Material theme admonition boxes (`!!! note`, `!!! warning`, `!!! tip`) are enabled for callout sections

**Site Structure:**
```
docs/
├── index.md                  ← Landing page: what you'll build, schedule, Start Lab 1 CTA
├── 00-prerequisites.md       ← Full setup guide: Java, Maven, CLI, Docker, OpenAI key
├── lab1-rest.md
├── lab2-panache.md
├── lab3-config-health.md
├── lab4-kafka.md
├── lab5-security.md
├── lab6-fault-tolerance.md
├── lab7-langchain4j.md
├── wrap-up.md                ← Native demo + next steps + resources
└── facilitator-guide.md      ← Instructor-only: timing cues, pitfalls, what to emphasise
```

**Todo List:**
1. Create `mkdocs.yml` with:
   - `site_name: "Quarkus Workshop: From Zero to AI-Powered Microservices"`
   - `theme: material` with `palette`, `features: navigation.tabs`, `content.code.copy: true`
   - Full `nav:` tree covering all 9 pages listed above
   - `plugins: search` enabled
   - `markdown_extensions: admonition, pymdownx.superfences, pymdownx.highlight` (enables code copy + admonition boxes)
2. Create `.github/workflows/deploy.yml`:
   - Trigger: `push` to `main`
   - Steps: checkout → setup Python → `pip install mkdocs-material` → `mkdocs gh-deploy --force`
3. Create `docs/index.md` — landing page with:
   - Workshop title and subtitle
   - "What you'll build" section (Coffee Shop diagram in text/table form)
   - 60-minute schedule table
   - Prerequisites summary (link to `00-prerequisites.md`)
   - Prominent "→ Start Lab 1" link
4. Create `docs/00-prerequisites.md` — full setup page with:
   - Java 21 install commands (Mac: `sdk install java 21`, Windows: link to Adoptium)
   - Quarkus CLI install (`sdk install quarkus` / `brew install quarkus`)
   - Docker Desktop / Podman install note (required for Labs 4 & 5 only)
   - OpenAI API key setup (`.env` file instructions)
   - `prereq-check.sh` script (inline in the page + downloadable link)
   - IDE setup: VS Code Quarkus extension or IntelliJ
5. Create stub `docs/` pages for all 7 labs and `wrap-up.md` (content filled in Sub-Tasks 1–8)
6. Add root `README.md` pointing to the live GitHub Pages URL

**Relevant Context:**
- MkDocs Material is the best fit: pure Markdown → production-quality site, built-in code copy buttons, admonition boxes, no Liquid template conflicts with Java code snippets (unlike Jekyll)
- `mkdocs gh-deploy --force` builds the site and force-pushes the `site/` output to the `gh-pages` branch — no manual steps
- `pymdownx.superfences` enables fenced code blocks with language highlighting; `pymdownx.highlight` adds line numbers and copy buttons
- Material admonitions: `!!! note "Title"`, `!!! warning`, `!!! tip` render as coloured callout boxes — use these for "What just happened?" and "Watch out" sections

**Status:** [x] done

---

### Sub-Task 0b — Workshop Scaffolding & Instructor Utilities

**Intent:** Create the repo skeleton, facilitator guide, and helper scripts that support the workshop operationally (instructor timing, attendee catch-up, environment validation).

**Expected Outcomes:**
- Per-lab `solution/` directories under `labs/` contain complete, runnable Maven projects attendees can copy if they fall behind
- `facilitator-guide.md` (published as a docs page) gives the instructor per-lab timing cues and common pitfalls
- `prereq-check.sh` validates the attendee's environment before the session starts
- `.env.example` documents required environment variables

**Repo Structure:**
```
quarkus-workshop/
├── mkdocs.yml
├── README.md                 ← points to GitHub Pages URL
├── .env.example
├── prereq-check.sh
├── docs/                     ← MkDocs source pages (the website)
│   └── ...
├── labs/
│   ├── lab1-rest/
│   │   └── solution/         ← complete Maven project
│   ├── lab2-panache/
│   │   └── solution/
│   ├── lab3-config-health/
│   │   └── solution/
│   ├── lab4-kafka/
│   │   └── solution/         ← order-service + menu-service both here
│   ├── lab5-security/
│   │   └── solution/
│   ├── lab6-fault-tolerance/
│   │   └── solution/
│   └── lab7-langchain4j/
│       ├── solution/
│       └── solution-with-rag/
└── .github/
    └── workflows/
        └── deploy.yml
```

**Todo List:**
1. Create `prereq-check.sh` — checks: `java -version` ≥ 21, `mvn -v` ≥ 3.9 or `quarkus version`, `docker ps` or `podman ps` succeeds, `OPENAI_API_KEY` env var is set; prints green/red per check
2. Create `.env.example`:
   ```
   OPENAI_API_KEY=sk-...
   QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-...
   ```
3. Create root `README.md` with one-liner: "Visit the workshop at https://<username>.github.io/<repo>/"
4. Create `labs/` skeleton directories (empty `solution/` stubs — content added per lab sub-task)
5. Create `docs/facilitator-guide.md` with: per-lab timing table, "pause and explain" callout points, expected attendee questions, Docker/Podman troubleshooting steps, OpenAI fallback plan

**Relevant Context:**
- All `solution/` projects are standalone Maven projects with their own `pom.xml` — attendees can `cd labs/lab1-rest/solution && quarkus dev` to run any solution directly
- The Coffee Shop theme (`menu-service`, `order-service`, `barista-bot`) ties labs together narratively
- Keep `labs/` and `docs/` as sibling directories — `docs/` is the website source, `labs/` is the runnable code

**Status:** [x] done

---

### Sub-Task 1 — Lab 1: First REST API, Dev Mode, Live Coding & Continuous Testing

**Intent:** Give attendees their first "wow" moment with Quarkus: scaffold a project in seconds, edit code without restarting, and run tests automatically in the terminal. This lab establishes the foundational Quarkus DX.

**Expected Outcomes:**
- Attendees bootstrap `menu-service` using `quarkus create app` or `code.quarkus.io`
- A `GET /menu` endpoint returns a hardcoded list of coffee items as JSON
- A `POST /menu` endpoint accepts a new item
- Dev Mode (`quarkus dev`) is running and attendees observe live reload after editing a response message
- Continuous testing (`quarkus dev` → press `r`) is demonstrated — a failing test turns green without restarting
- Swagger UI at `http://localhost:8080/q/swagger-ui` is used to test the endpoints interactively
- Attendees understand what extensions are and how to add them

**Todo List:**
1. Write `docs/lab1-rest.md` as a **fully prose page** — every step must include:
   - The exact terminal command to run (in a fenced `bash` code block with copy button)
   - The complete Java file content (in a fenced `java` code block) — no partial snippets
   - A `!!! note "What just happened?"` admonition after each significant step explaining the concept
   - Step 1: Bootstrap — full `quarkus create app` command, then annotated project tree showing what each file is for
   - Step 2: Create `MenuItem.java` — complete class with fields `name`, `description`, `price`
   - Step 3: Create `MenuResource.java` — complete class with `@Path`, `@GET`, `@POST`, `@Produces`, `@Consumes`
   - Step 4: Start Dev Mode — `quarkus dev` command, screenshot description of the terminal output, explain what "Listening on: http://localhost:8080" means
   - Step 5: Live coding demo — exact edit to make (change a field value), save instruction, "now refresh Swagger UI — no restart needed" with a `!!! tip` callout explaining live reload mechanism
   - Step 6: Continuous testing — press `r` instruction, complete `MenuResourceTest.java` with `@QuarkusTest` and RestAssured assertion, watch it turn green — `!!! note` explaining that tests run on every save
   - Step 7: Swagger UI tour — navigate to `http://localhost:8080/q/swagger-ui`, try `GET /menu` and `POST /menu` from the browser
2. Create `labs/lab1-rest/solution/` with `MenuItem.java`, `MenuResource.java`, `MenuResourceTest.java` — complete, compilable, runnable

**Relevant Context:**
- Extension: `quarkus-rest-jackson` (replaces RESTEasy Reactive in Quarkus 3.x) — artifact `io.quarkus:quarkus-rest-jackson`
- Extension: `quarkus-smallrye-openapi` — auto-generates OpenAPI spec + Swagger UI
- `quarkus dev` starts on port 8080 by default; Dev UI at `/q/dev-ui`
- Continuous testing is triggered by pressing `r` in the running `quarkus dev` terminal
- Live reload: Quarkus detects source changes on the next HTTP request — no manual restart
- `@QuarkusTest` + `RestAssured` are included by default in generated projects

**Status:** [x] done

---

### Sub-Task 2 — Lab 2: Panache ORM + H2 In-Memory Database

**Intent:** Show how Quarkus makes persistence trivial with Panache Active Record — no DAO boilerplate, no XML config, H2 spins up in-process with zero setup.

**Expected Outcomes:**
- `MenuItem` is promoted from a POJO to a JPA `@Entity` extending `PanacheEntity`
- `GET /menu` now reads from the database; `POST /menu` persists a new item
- H2 is configured in `application.properties` for the dev profile
- Attendees observe that Hibernate auto-creates the schema on startup
- Dev UI database browser (Agroal DevUI) is briefly shown

**Todo List:**
1. Write `docs/lab2-panache.md` as a **fully prose page** — every step must include complete code and a "What just happened?" admonition:
   - Step 1: Add extensions — exact `quarkus ext add` command shown, then the resulting `pom.xml` dependency block to confirm
   - Step 2: Promote `MenuItem.java` to a JPA entity — complete updated file showing `@Entity`, `extends PanacheEntity`, `@Column` fields; `!!! note` explaining that `PanacheEntity` provides `id`, `persist()`, `listAll()` etc. for free
   - Step 3: Update `MenuResource.java` — complete file diff showing `MenuItem.listAll()` and `@Transactional item.persist()`; `!!! warning "Don't forget @Transactional"` admonition
   - Step 4: Complete `application.properties` block for H2 with all three properties; `!!! tip` noting H2 is dev-only and prod would use PostgreSQL (which DevServices can also auto-start)
   - Step 5: Complete `import.sql` content with 3 seed rows (Espresso, Cappuccino, Cold Brew)
   - Step 6: `!!! note "Active Record vs Repository"` callout box explaining both patterns; this lab uses Active Record
   - Step 7: Show the Dev UI Agroal panel — navigate to `http://localhost:8080/q/dev-ui`, find the datasource browser, run a query
2. Create `labs/lab2-panache/solution/` — complete Maven project with all updated files plus `import.sql`

**Relevant Context:**
- `PanacheEntity` provides `id` field automatically; `PanacheEntityBase` is used when you supply your own id
- `@Transactional` must be on the resource method (or a service layer) for write operations
- H2 in-memory DB is dev/test only — note in the lab that prod would use PostgreSQL (DevServices auto-provides one too)
- `quarkus.hibernate-orm.log.sql=true` is a useful dev tip to show generated SQL

**Status:** [x] done

---

### Sub-Task 3 — Lab 3: Config Profiles + Health Checks + Dev UI Tour

**Intent:** Show Quarkus' config system (profiles, `@ConfigProperty`), production-readiness health endpoints, and give a guided tour of the Dev UI — one of Quarkus' most distinctive features.

**Expected Outcomes:**
- A `%dev`, `%test`, `%prod` config profile is demonstrated — e.g., different welcome messages or log levels per environment
- `@ConfigProperty` injects a `coffee.shop.name` property into the resource response
- `/q/health/live` and `/q/health/ready` return UP with zero extra code
- A custom `@Liveness` check is added (e.g., checks if the menu has at least one item)
- Guided Dev UI tour covers: Extensions tab, Configuration tab, Hibernate Panache browser, Swagger UI, Continuous Testing panel

**Todo List:**
1. Write `docs/lab3-config-health.md` as a **fully prose page**:
   - Step 1: `@ConfigProperty` — complete `application.properties` snippet adding `coffee.shop.name`, then complete updated `MenuResource.java` showing the injection and a new `GET /menu/info` endpoint returning the shop name; `!!! note` explaining `defaultValue` and `Optional<T>` variants
   - Step 2: Config profiles — complete `application.properties` block showing `coffee.shop.name`, `%dev.coffee.shop.name`, `%prod.coffee.shop.name`; `!!! tip` explaining how to activate prod profile with `-Dquarkus.profile=prod`
   - Step 3: Add health extension — exact `quarkus ext add` command; then `curl http://localhost:8080/q/health` response shown as a JSON code block; explain liveness vs readiness
   - Step 4: Custom health check — complete `CoffeeShopHealthCheck.java` implementing `HealthCheck` with `@Liveness`; `!!! note` explaining the `HealthCheckResponse` builder
   - Step 5: Dev UI tour — numbered walkthrough of each panel with what to click and what to look for: Extensions, Configuration, Hibernate Panache browser, Continuous Testing, OpenAPI; `!!! tip "Dev UI is dev-only"` callout
2. Create `labs/lab3-config-health/solution/` — updated `MenuResource.java`, full `application.properties`, `CoffeeShopHealthCheck.java`

**Relevant Context:**
- Profile activation: `%dev.`, `%test.`, `%prod.` prefixes in `application.properties`
- `@ConfigProperty` supports `defaultValue` and `Optional<T>`
- `quarkus-smallrye-health` auto-registers readiness checks for datasource connectivity
- Dev UI is only available in Dev Mode — it does not ship in production builds
- Custom health check: implement `org.eclipse.microprofile.health.HealthCheck`, annotate with `@Liveness` or `@Readiness`

**Status:** [x] done

---

### Sub-Task 4 — Lab 4: Kafka Messaging with DevServices

**Intent:** Demonstrate event-driven messaging with SmallRye Reactive Messaging and show the DevServices "magic" — adding a dependency auto-starts a Kafka broker with zero configuration.

**Expected Outcomes:**
- A new `order-service` project is created
- `POST /orders` publishes an `OrderPlaced` event to a Kafka topic `coffee-orders`
- `menu-service` has a consumer that logs (or updates a counter for) incoming orders
- Attendees see in the Dev UI (Kafka panel) that messages are flowing
- No `docker-compose.yml` or Kafka config is written — DevServices handles it

**Todo List:**
1. Write `docs/lab4-kafka.md` as a **fully prose page**:
   - Intro section: concept diagram (ASCII art) showing `order-service → Kafka topic → menu-service`; `!!! note "What is DevServices?"` box explaining the zero-config Kafka magic
   - Step 1: Create `order-service` — full `quarkus create app` command; complete `Order.java` POJO
   - Step 2: Complete `OrderResource.java` with `@Channel` injection and `Emitter.send()` call — include the Jackson `ObjectMapper` serialisation to JSON string
   - Step 3: Complete `order-service/application.properties` with outgoing channel config block
   - Step 4: Add Kafka extension to `menu-service` — exact command; complete `OrderConsumer.java` with `@Incoming("coffee-orders")` and `@ApplicationScoped`; complete consumer `application.properties` block
   - Step 5: Start both services in two terminals — exact commands; `!!! warning "Docker must be running"` admonition
   - Step 6: POST an order to `order-service`, see it logged in `menu-service` terminal — show both terminal outputs as code blocks
   - Step 7: Dev UI Kafka panel — navigate `http://localhost:8080/q/dev-ui`, find Kafka, send a test message from the UI
   - `!!! note "Notice what you didn't do"` closing callout: no `docker run`, no `docker-compose.yml`, no Kafka config file
2. Create `labs/lab4-kafka/solution/order-service/` and `labs/lab4-kafka/solution/menu-service/` — both complete Maven projects

**Relevant Context:**
- Extension: `io.quarkus:quarkus-smallrye-reactive-messaging-kafka` (newer alias: `quarkus-messaging-kafka`)
- DevServices starts a **Redpanda** container (Kafka-compatible, lighter than full Kafka) automatically
- Requires Docker or Podman running on the laptop — flag this clearly as a prerequisite
- `@Channel` + `Emitter` is the imperative way to send; `@Outgoing` annotation is the reactive way
- In Dev Mode, both services can be run in separate terminals and DevServices will share the same broker

**Status:** [x] done

---

### Sub-Task 5 — Lab 5: OIDC Security + Keycloak DevServices

**Intent:** Show how Quarkus + DevServices for Keycloak makes adding production-grade OAuth2/OIDC security to an API a configuration-only exercise — no manual Keycloak setup, no realm import files.

**Expected Outcomes:**
- `POST /menu` (add a new item) is protected — returns HTTP 401 without a token
- Attendees obtain a JWT token from the DevServices Keycloak via the Dev UI "OpenID Connect" panel
- A valid token allows `POST /menu` to succeed (HTTP 200/201)
- `@RolesAllowed("barista")` is used to restrict the endpoint further — only users with the `barista` role can add items
- The Keycloak Dev UI panel shows users and roles auto-created by DevServices

**Todo List:**
1. Write `docs/lab5-security.md` as a **fully prose page**:
   - Intro section: `!!! note "What is OIDC?"` — one paragraph explaining bearer tokens, why we don't roll our own auth
   - Step 1: Add `quarkus-oidc` extension — exact command; complete minimal `application.properties` block with `quarkus.oidc.application-type=service`; `!!! note "DevServices magic"` box explaining that the absence of `auth-server-url` triggers auto-start of Keycloak
   - Step 2: Protect the endpoint — complete updated `MenuResource.java` showing `@Authenticated` on `POST /menu` and `@RolesAllowed("barista")` on a new `POST /menu/admin` endpoint; imports listed explicitly
   - Step 3: Start `quarkus dev` — `!!! warning "Docker must be running"` admonition; show the startup log line confirming Keycloak started
   - Step 4: Get a token from Dev UI — step-by-step click path through `http://localhost:8080/q/dev-ui` → OpenID Connect panel → log in as `alice` / `alice`; show the token copy button
   - Step 5: Test with curl — complete curl commands as code blocks: (a) no token → 401 response block, (b) with token → 200 response block, (c) alice hitting `/menu/admin` → 403 response block
   - `!!! note "What DevServices created"` closing callout: realm name, client ID, test users `alice`/`bob` with their roles
2. Create `labs/lab5-security/solution/` — complete Maven project with updated `MenuResource.java` and `application.properties`

**Relevant Context:**
- Extension: `io.quarkus:quarkus-oidc`
- DevServices Keycloak requires Docker or Podman running
- Default DevServices test users: `alice` (user role), `bob` (admin role) — both with password `alice`/`bob`
- `@Authenticated` is from `io.quarkus.security.Authenticated`; `@RolesAllowed` is from `jakarta.annotation.security`
- `quarkus.oidc.application-type=service` tells Quarkus this is a bearer token API (not a web app with login pages)
- JWT token can be obtained from Dev UI without writing any test code — great for live demo

**Status:** [x] done

---

### Sub-Task 6 — Lab 6: Fault Tolerance

**Intent:** Show that adding resilience patterns (`@Retry`, `@Fallback`, `@Timeout`) to any CDI bean requires only annotations — no framework boilerplate. Use a realistic scenario: the menu service calling an (unreliable) external pricing service.

**Expected Outcomes:**
- A `PricingService` bean simulates an unreliable external call (randomly throws an exception)
- `@Retry(maxRetries = 3)` is added — attendees see in logs that retries happen automatically
- `@Fallback(fallbackMethod = "defaultPrice")` provides a hardcoded price when all retries are exhausted
- `@Timeout(value = 2, unit = ChronoUnit.SECONDS)` cancels calls that take too long
- The `GET /menu/{id}/price` endpoint remains stable despite the unreliable backend

**Todo List:**
1. Write `docs/lab6-fault-tolerance.md` as a **fully prose page**:
   - Intro section: `!!! note "Why fault tolerance?"` — explain that microservices call each other over the network; networks fail; we need retry + fallback patterns without writing retry loops
   - Step 1: Add extension — exact `quarkus ext add` command
   - Step 2: Complete `PricingService.java` — full class with `@ApplicationScoped`, `getPrice(Long itemId)` method with `Math.random() < 0.5` failure simulation, `Logger` logging each attempt; `defaultPrice()` fallback method
   - Step 3: Show annotations added to `getPrice()` — `@Retry`, `@Fallback`, `@Timeout` each shown with their parameters; `!!! note` explaining the execution order (Timeout wraps Retry wraps the method; Fallback is the last resort)
   - Step 4: Complete updated `MenuResource.java` showing `GET /menu/{id}/price` endpoint calling `PricingService`
   - Step 5: Complete `application.properties` snippet enabling DEBUG logging for the package
   - Step 6: Test it — hit the endpoint 5–6 times via Swagger UI; show example terminal output with retry log lines and an occasional fallback; `!!! tip "Circuit Breaker"` callout mentioning `@CircuitBreaker` as a natural next step with a link to the Quarkus guide
2. Create `labs/lab6-fault-tolerance/solution/` — complete Maven project with `PricingService.java` and updated `MenuResource.java`

**Relevant Context:**
- Extension: `io.quarkus:quarkus-smallrye-fault-tolerance`
- All fault tolerance annotations are from `org.eclipse.microprofile.faulttolerance`
- `@Retry` and `@Fallback` work together — fallback fires only after all retries are exhausted
- The bean must be a CDI bean (`@ApplicationScoped` / `@RequestScoped`) for annotations to be intercepted
- Keep the demo simple — resist the urge to demo `@CircuitBreaker` (adds complexity for 5 min slot)

**Status:** [x] done

---

### Sub-Task 7 — Lab 7: LangChain4j AI Chatbot + Easy RAG

**Intent:** Show how Quarkus LangChain4j turns LLM integration into a type-safe Java interface — no HTTP client boilerplate, no JSON parsing. Add a menu-aware AI barista in ~20 lines of code, then extend it with document Q&A (Easy RAG) as a bonus.

**Expected Outcomes:**
- `barista-bot` project is created with `quarkus-langchain4j-openai`
- A `BaristaAiService` interface with `@RegisterAiService` and `@SystemMessage` is defined
- `GET /chat?message=...` endpoint proxies user messages to OpenAI via the AI service
- The bot responds in-character as a coffee shop barista (system prompt configures persona)
- **Bonus — Easy RAG:** A `menu.txt` file (list of all coffee items with descriptions) is placed in `src/main/resources/`; `quarkus-langchain4j-easy-rag` extension ingests it automatically; the bot can now answer "Do you have oat milk?" without hallucinating
- OpenAI API key is set via environment variable `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY`

**Todo List:**
1. Write `docs/lab7-langchain4j.md` as a **fully prose page**:
   - Intro section: `!!! note "What is LangChain4j?"` — explain AI services as type-safe Java interfaces; no HTTP clients, no JSON parsing, no API keys scattered through code
   - Step 1: Create `barista-bot` project — full `quarkus create app` command
   - Step 2: Add LangChain4j dependencies — complete `pom.xml` `dependencyManagement` block adding `quarkus-langchain4j-bom`, then the `quarkus-langchain4j-openai` dependency; `!!! warning "Group ID is io.quarkiverse.langchain4j — not io.quarkus"` admonition
   - Step 3: Set the API key — complete `.env` file snippet and the `export` command for Mac/Linux; `!!! tip` showing the Windows `set` equivalent
   - Step 4: Complete `BaristaAiService.java` — full interface with all annotations; `!!! note` explaining what each annotation (`@RegisterAiService`, `@ApplicationScoped`, `@SystemMessage`, `@UserMessage`) does
   - Step 5: Complete `ChatResource.java` — full class with injection, `GET /chat` endpoint, `@RestQuery String message` parameter
   - Step 6: Complete `application.properties` block with model name and logging flags
   - Step 7: Test via Swagger UI — example question shown; example AI response shown as a block quote
   - **Bonus RAG section** (clearly marked as optional):
     - Step 8: Add `quarkus-langchain4j-easy-rag` — exact command
     - Step 9: Complete `menu.txt` content (all 5 coffee items with descriptions and prices)
     - Step 10: Add `quarkus.langchain4j.easy-rag.path` property — complete snippet
     - Step 11: Ask "Do you have a cold brew?" before and after RAG — show both responses; `!!! note "What Easy RAG does"` explaining the vector store + similarity search pipeline in plain language
2. Create `labs/lab7-langchain4j/solution/` — complete `barista-bot` project without RAG
3. Create `labs/lab7-langchain4j/solution-with-rag/` — complete `barista-bot` project with RAG and `menu.txt`

**Relevant Context:**
- Group ID: `io.quarkiverse.langchain4j` (NOT `io.quarkus`) — this is a Quarkiverse extension
- BOM artifact: `io.quarkiverse.langchain4j:quarkus-langchain4j-bom` — add to `dependencyManagement` section
- Runtime artifact: `io.quarkiverse.langchain4j:quarkus-langchain4j-openai`
- Easy RAG artifact: `io.quarkiverse.langchain4j:quarkus-langchain4j-easy-rag`
- API key env var: `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY` (or property `quarkus.langchain4j.openai.api-key`)
- `@RegisterAiService` beans are `@RequestScoped` by default; annotate with `@ApplicationScoped` for shared state
- `@SystemMessage` can be on the interface (applies to all methods) or on individual methods
- Easy RAG auto-ingests files from `src/main/resources` — no vector DB setup needed for the demo

**Status:** [ ] pending

---

### Sub-Task 8 — Wrap-Up: Native Image Demo + Resources Slide

**Intent:** Close the workshop with the "final wow moment" — a pre-built native executable starting in milliseconds vs the JVM — and give attendees clear next steps.

**Expected Outcomes:**
- Instructor shows a terminal with two side-by-side startup logs: JVM (~0.8s) vs native (~0.02s) from a pre-built demo app
- Attendees understand how to build their own native image: `quarkus build --native` (no GraalVM install needed with the container build flag)
- A `resources.md` file lists curated next-step links
- Attendees know where to go to continue learning

**Todo List:**
1. Write `docs/wrap-up.md` as a **fully prose page**:
   - Native image section: show the build command (`quarkus build --native -Dquarkus.native.container-build=true`) in a fenced `bash` block; explain the container build flag; show side-by-side startup time comparison as a table (JVM ~800ms vs Native ~20ms); `!!! note "Don't run this now"` admonition explaining it takes 3+ minutes and the instructor has a pre-built binary to demo
   - Show the startup log lines from JVM and native as two separate code blocks so attendees see the difference visually
   - Next steps section: formatted table of curated resources with descriptions — `quarkus.io/guides`, `code.quarkus.io`, `quarkus.io/quarkus-workshop-langchain4j`, `quarkiverse.io`, IBM Enterprise Build of Quarkus docs
   - `!!! tip "IBM Enterprise Build of Quarkus"` callout explaining the production-supported IBM path
2. Update `docs/facilitator-guide.md` (from Sub-Task 0b) to add the scripted native demo section: exact terminal commands to run before the workshop to pre-build the binary, and the demo script for the side-by-side comparison

**Relevant Context:**
- Native build with container: `quarkus build --native -Dquarkus.native.container-build=true` uses a builder image — no local GraalVM install needed
- Typical JVM startup for `menu-service`: ~800ms; typical native startup: ~15–30ms
- Keep this segment under 5 minutes — it is a demo, not a lab
- Mention IBM Enterprise Build of Quarkus as the production-supported path for IBM customers

**Status:** [x] done

---

## Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| REST extension | `quarkus-rest-jackson` | Current Quarkus 3.x standard; replaces RESTEasy Reactive |
| Database (dev) | H2 in-memory | Zero setup, no Docker needed for Lab 2 |
| Kafka DevServices | Redpanda (auto) | Lighter than full Kafka; auto-started by adding extension |
| OIDC DevServices | Keycloak (auto) | Full Keycloak auto-started with test users; zero realm config |
| LLM provider | OpenAI API | Reliable, well-documented; requires internet + API key |
| LangChain4j BOM | `io.quarkiverse.langchain4j:quarkus-langchain4j-bom` | Correct group ID — NOT `io.quarkus` |
| Skip virtual threads | — | Excluded per workshop design decision |
| Skip reactive (Uni/Multi) | — | Too much paradigm shift for a 60-min beginner session |
| Skip GraphQL/gRPC | — | Out of scope for this workshop duration |

## Open Questions / Notes for Facilitators

- **Docker/Podman requirement:** Labs 4 and 5 require a container runtime. Add a "pre-flight check" at the start of the session (`docker ps` or `podman ps` must succeed).
- **OpenAI latency:** Lab 7 responses depend on OpenAI API speed — have a fallback demo recorded in case of network issues.
- **Attendee pace:** Labs 1–3 are the slowest because setup is happening. Labs 4–7 go faster since the pattern is familiar. The facilitator guide should note where attendees can skip ahead using `solution/` dirs.
- **IBM Enterprise Quarkus:** Mention at the wrap-up that IBM provides a supported enterprise build — refer to the `migrate-community-to-ibm-quarkus` skill for migration steps.
