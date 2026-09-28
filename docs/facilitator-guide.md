# Facilitator Guide

!!! note "For instructors only"
    This page is for workshop facilitators. It contains timing cues, "pause and explain" callout points, common attendee questions, and troubleshooting tips. Attendees are welcome to read it, but it won't be useful during the labs.

---

## Timing Overview

| Time | Section | What to do |
|------|---------|-----------|
| -15 min | Pre-session | Confirm Docker/Podman is running on your machine. Pre-build native binary (see Wrap-Up). Have a browser tab open to `code.quarkus.io` as a backup. |
| 00:00 – 05:00 | Intro | Slides/whiteboard: Why Quarkus, the Coffee Shop theme, what each lab builds. Keep it short — attendees are eager to code. |
| 05:00 – 17:00 | Lab 1 | **Slowest lab** — project creation and IDE setup takes time. Circulate the room. Shout "press `r` now" when it's time for continuous testing. |
| 17:00 – 25:00 | Lab 2 | Faster once IDE is set up. Key moment: show `PanacheEntity.listAll()` — one line replacing 20 lines of DAO code. |
| 25:00 – 31:00 | Lab 3 | Dev UI tour is the highlight here. Take 2 minutes to click through every panel — don't rush it. |
| 31:00 – 38:00 | Lab 4 | **Requires Docker/Podman running.** Pause before starting and confirm everyone's daemon is up. The "zero Kafka config" moment lands well with experienced devs. |
| 38:00 – 45:00 | Lab 5 | **Requires Docker/Podman running.** Keycloak takes ~15s to start — warn attendees to expect a brief wait on first `quarkus dev`. The 401 → 200 curl demo is satisfying. |
| 45:00 – 50:00 | Lab 6 | Fast lab. Key teaching point: annotations intercept CDI beans — only works on `@ApplicationScoped` etc. Run the endpoint 5+ times so retries are visible. |
| 50:00 – 58:00 | Lab 7 | AI responses vary. If OpenAI is slow, narrate what's happening. RAG bonus is optional — skip if behind on time. |
| 58:00 – 60:00 | Wrap-Up | Native demo only. Don't try to compile live. Show pre-built binary only. |

The core workshop is 60 minutes. Labs 8, 9, and 10 are **optional extensions** for a longer session or self-paced follow-up — budget roughly:

| Lab | Budget | What to do |
|-----|--------|-----------|
| Lab 8 *(optional)* | ~10 min | **Requires the OpenAI key from Lab 7.** Two terminals, two ports. The "LLM picked the right tool by itself" moment is the payoff — make sure `log-requests=true` output is visible. |
| Lab 9 *(optional)* | ~10 min | **Requires Docker/Podman. No Kubernetes cluster needed.** Have attendees pre-pull the UBI9 base image during the previous break — the first `podman build` is otherwise a long download. |
| Lab 10 *(optional)* | ~12 min | **Requires Labs 4 and 8, Docker/Podman, and the OpenAI key.** Four services on four ports — use `start-services.sh` rather than four hand-managed terminals. |

---

## Per-Lab Timing Notes & Pitfalls

### Intro (00:00 – 05:00)

**What to say:**
> "Quarkus is a Java framework that feels very different from Spring Boot the moment you start it. Today we're going to feel that difference, not just read about it. By the end of the hour you'll have a REST API, a database, Kafka, security, and an AI chatbot — all running on your laptop."

**Pause and explain:** Walk through the use case on screen or on the whiteboard. The Quarkus Cafe has three services: `menu-service` (menu REST API + DB + security), `order-service` (Kafka producer for customer orders), and `barista-bot` (AI assistant grounded in the actual menu). Customers browse the menu and chat with the bot; staff place orders and manage the menu. All three talk to real infrastructure that Quarkus DevServices starts automatically.

---

### Lab 1 — First REST API (05:00 – 17:00)

**Key moments to call out:**
- After `quarkus create app` finishes: "Notice it created a complete working project in seconds. Open `pom.xml` — your extensions are already there."
- After first `quarkus dev` starts: "It's running. Now — don't restart it for the rest of this lab. We won't need to."
- After the live reload demo: Pause. Say: "Did anyone restart the server? No. Quarkus detected the change and recompiled on your next request. This is live reload."
- After pressing `r` for continuous testing: "Now watch — every time you save a file, the tests re-run. You don't have to think about running tests. They just run."

**Common questions:**
- *"What's the difference between `quarkus-rest` and `quarkus-resteasy`?"* — `quarkus-rest` is the modern name as of Quarkus 3.x. Same technology, just renamed.
- *"Can I use `@RestController` like in Spring?"* — Yes, with `quarkus-spring-web`, but in this workshop we use native Quarkus annotations.

**Pitfalls:**
- Port 8080 already in use — ask attendees to run `lsof -i :8080` and kill the process.
- IDE not resolving `@Path` imports — make sure the Maven project is imported/synced.

---

### Lab 2 — Panache ORM (17:00 – 25:00)

**Key moments to call out:**
- After `extends PanacheEntity`: "That's it. No DAO. No EntityManager injection. No boilerplate `findById` method. Panache gives you all of those for free as static methods on your entity class."
- After seed data loads: "Open the Dev UI database browser — `http://localhost:8080/q/dev-ui`. You can run SQL queries live against your H2 database without any tooling."

**Common questions:**
- *"What about production? H2 is just for testing right?"* — Correct. In prod you'd use PostgreSQL. If you add `quarkus-jdbc-postgresql` instead of H2, DevServices auto-starts a Postgres container for dev. Same pattern.
- *"What's the Repository pattern vs Active Record?"* — Active Record (this lab) puts the DB methods on the entity itself. Repository separates them. Both work in Quarkus.

**Pitfalls:**
- Forgetting `@Transactional` on POST → `jakarta.persistence.TransactionRequiredException`. Show the error message and the fix.
- `import.sql` not picked up — must be in `src/main/resources`, not `src/test/resources`.

---

### Lab 3 — Config & Health (25:00 – 31:00)

**Key moments to call out:**
- After the profile demo: "Notice `%prod.coffee.shop.name` overrides the default — only when you run with the prod profile. Zero code change needed to configure differently per environment."
- During Dev UI tour: slow down here. Open every panel. Show the Configuration editor (live config change without restart), show the Hibernate entity browser, show the Continuous Testing panel.

!!! tip "Dev UI is the best demo in the workshop"
    Attendees who come from Spring Boot are genuinely surprised by this. Take an extra minute here if the pacing allows.

**Common questions:**
- *"Is Dev UI in production?"* — No. It's completely stripped from production builds. Dev only.
- *"Can I add my own panels to Dev UI?"* — Yes, via the extension API. Out of scope today.

---

### Lab 4 — Kafka with DevServices (31:00 – 38:00)

!!! warning "Pre-flight check"
    Before starting this lab, confirm Docker Desktop or Podman is running on every attendee's laptop. Ask them to run `docker ps` or `podman ps` and confirm they see an empty table (not an error). Remind them that `prereq-check.sh` times out after 5 seconds — if it reported "not running" but the daemon has since started, that's fine.

**Key moments to call out:**
- Before running `quarkus dev`: "We haven't installed Kafka. We haven't written a `docker-compose.yml`. We haven't set a broker URL. Watch what happens when we just add the extension."
- After first `quarkus dev` start: Point to the log line: `Dev Services for Kafka started`. "Quarkus downloaded and started a Redpanda container automatically. That is DevServices."
- After sending an order: Switch terminal windows — show the consumer log line in `menu-service`. "Two services, one Kafka broker, zero config."

**Common questions:**
- *"What is Redpanda?"* — A Kafka-compatible broker that's faster and lighter than Apache Kafka. Quarkus uses it for DevServices because it starts in under 2 seconds.
- *"How do they share the same broker?"* — DevServices detects that both services are running in dev mode and connects them to the same container automatically.

**Pitfalls:**
- Docker not running → `Could not connect to Docker` error. Start Docker/Podman first.
- Port conflict on 9092 if a real Kafka is already running locally — DevServices will use a random port. Usually auto-resolves.

---

### Lab 5 — OIDC Security (38:00 – 45:00)

!!! warning "Pre-flight check"
    Docker/Podman must still be running from Lab 4. Keycloak takes ~15 seconds to start on first `quarkus dev` — warn attendees to expect the wait.

**Key moments to call out:**
- After adding `quarkus-oidc` and restarting: Point to `Dev Services for Keycloak started` in the log. "Full Keycloak — with a realm, a client, and test users — started automatically."
- The 401 without a token: Run the curl command first with no token. Show the 401. "Without a token, the request is rejected before your code ever runs."
- Getting the token from Dev UI: This is the best UI moment of the lab. Walk through it step by step — the OIDC panel is intuitive.
- The 403 for wrong role: Show alice (user role) hitting `POST /menu/admin` — which requires the `admin` role. "Authenticated — yes. Authorised — no. Different error, different meaning."

**Common questions:**
- *"Where are the Keycloak users defined?"* — DevServices creates them automatically: `alice` with `user` role, `bob` with `admin` role. The `POST /menu` endpoint accepts any valid token (`@Authenticated`); `POST /menu/admin` requires the `admin` role (`@RolesAllowed("admin")`).
- *"What happens in production?"* — You set `quarkus.oidc.auth-server-url` to your real Keycloak/OIDC provider. Everything else stays the same.
- *"Do we need to write login pages?"* — No, for a REST API (bearer token flow). If you needed a web app with login pages, you'd use `application-type=web-app`.

**Pitfalls:**
- Keycloak slow to start — just wait. It will come up.
- Token expired during demo — tokens from Dev UI expire in a few minutes. Re-fetch from the Dev UI panel if `curl` starts returning 401 again.

---

### Lab 6 — Fault Tolerance (45:00 – 50:00)

**Key moments to call out:**
- Show `Math.random() < 0.5` in `PricingService` — "We're simulating a flaky network call. Hit the endpoint 6 times and watch the retry logs."
- After showing logs with retries: "Notice your endpoint returned successfully every time despite the 50% failure rate. That's `@Retry` working."
- After `@Fallback`: Hit the endpoint until a fallback fires. "When all retries are exhausted, `defaultPrice()` kicks in. The user gets a response — not a 500."

**Common questions:**
- *"What about `@CircuitBreaker`?"* — It builds on `@Retry` — after N consecutive failures, it opens the circuit and fails fast (no retries) for a time window. Mention it exists, point to the Quarkus guide, skip implementing it today.
- *"Does this work with async calls?"* — Yes. All annotations work with `CompletionStage` and Mutiny `Uni` return types.

---

### Lab 7 — LangChain4j (50:00 – 58:00)

!!! warning "OpenAI latency"
    Responses from OpenAI can take 2–5 seconds. If the network is slow at the venue, have a screen recording of a working demo ready as a fallback.

**Key moments to call out:**
- After writing `BaristaAiService.java`: "This is the entire integration. One interface. Four annotations. No HTTP client. No JSON parsing. No API key in the code. Quarkus wires it all up."
- First response from Swagger UI: Pause and let the response appear. "That response was generated by GPT-4o-mini, called from our Java interface, returned as a plain `String`."
- After adding Easy RAG: Ask "Do you have oat milk?" before and after the RAG step. "Before RAG, the model could hallucinate. After RAG, it answers from our actual menu document."

**Common questions:**
- *"Why `io.quarkiverse.langchain4j` not `io.quarkus`?"* — The BOM is now under `io.quarkus.platform` (part of the Quarkus platform since 3.20), but the runtime JARs still use the `io.quarkiverse.langchain4j` group ID. The lab step adds both a `<properties>` entry and the BOM snippet — point attendees there if they get confused.
- *"Can I use other models?"* — Yes. Swap `quarkus-langchain4j-openai` for `quarkus-langchain4j-ollama` (local) or `quarkus-langchain4j-azure-openai`. The `BaristaAiService` interface stays unchanged.
- *"What is RAG?"* — Retrieval Augmented Generation. Instead of relying on the model's training data, you inject relevant documents into the context at query time. Easy RAG does the embedding and retrieval automatically.

---

### Lab 8 — MCP Server *(optional, ~10 min)*

!!! warning "Pre-flight check"
    Two projects, two terminals, two ports: `menu-mcp-server` on **8084** and `barista-bot` on **8080**. Attendees need the same OpenAI key as Lab 7. No Docker required for this lab.

!!! tip "Start the MCP server first"
    `barista-bot` resolves its MCP tools at startup. If the server on 8084 isn't up yet, the bot starts but has no tools and silently answers from the model's own knowledge — which looks like the lab "working" when it isn't. Get a green `Listening on: http://localhost:8084` before starting the bot.

**Key moments to call out:**

- After writing `MenuTools.java`: "Three methods, three `@Tool` annotations. That's a complete MCP server. No protocol code, no JSON-RPC handling, no transport wiring — the extension does all of it."
- Point at `@ToolArg`: "This description isn't a comment for humans. It's the text the LLM reads to decide what to put in the argument. Vague descriptions produce wrong tool calls — prompt engineering has moved into your method signatures."
- After adding `@McpToolBox("menu")` to `BaristaAiService`: "One annotation on the interface. The bot now has three new capabilities and we didn't write a single line of client code."
- **The payoff moment.** Ask the bot "How much is a flat white?" and switch to the `barista-bot` terminal. `log-requests=true` and `log-responses=true` are on, so the tool call and result are in the log. "Nobody told it to call `getItemPrice`. It read the tool descriptions, picked the right one, extracted the argument from plain English, and called it. That decision was the model's."
- Follow up with "Which drinks work with oat milk?" so a *different* tool fires. Two questions, two different tools, same code.

**Common questions:**

- *"How is this different from Lab 7's RAG?"* — RAG stuffs documents into the prompt before the model runs. Tools let the model *decide* to fetch something, at the moment it needs it, with arguments it chose. RAG is read-only context; tools can do anything a method can — including writes, which is exactly what Lab 10 builds on.
- *"Why a separate server instead of `@ToolBox` on a local bean?"* — You can absolutely use local tools (Lab 10 does). MCP earns its keep when the tools live in a different service, a different team's codebase, or a different language. The menu belongs to `menu-service`, not to the bot.
- *"Is MCP an OpenAI thing?"* — No, it's an open protocol from Anthropic, and it's model-agnostic. The same server works with Claude Desktop, an IDE, or any MCP client.
- *"What's SSE doing here?"* — `quarkus-mcp-server-sse` exposes the server over HTTP, on two endpoints: `/mcp` (streamable HTTP, what the client uses) and `/mcp/sse` (the older Server-Sent Events transport). There's also a stdio transport for locally-spawned servers. Watch for attendees pointing the client at `/mcp/sse` — it answers the streamable client's POST with a 405.
- *"Can the model call two tools in one turn?"* — Yes. Ask "What's the cheapest drink and does it come with oat milk?" if you have a spare minute.

**Pitfalls:**

- Port 8080 held by a `menu-service` from an earlier lab, so `barista-bot` can't bind. Press `q` in the old terminal first.
- Starting `barista-bot` before the MCP server — see the tip above. The symptom is a plausible-but-wrong answer, not an error.
- `@ToolArg` descriptions left vague or copied between tools → the model picks the wrong tool. Good demo of *why* the description matters if it happens: fix it live and re-ask.
- Attendees expecting deterministic answers. The model may paraphrase, round prices, or answer from memory if the question doesn't clearly need a tool. Ask a question only the tool can answer.
- Forgetting to re-export the OpenAI key in the *second* terminal.

---

### Lab 9 — Containerization *(optional, ~10 min)*

!!! warning "Pre-flight check"
    Podman (or Docker) must be running — confirm with `podman info`. **No Kubernetes cluster is needed for this lab**: attendees build an image and run it locally. Make sure Dev Mode from earlier labs is stopped (`q`) so port 8080 is free for the container.

!!! tip "Warm the base image beforehand"
    The first `podman build` pulls `registry.access.redhat.com/ubi9/openjdk-21` (~400 MB). On venue Wi-Fi that can take several minutes for a room full of people. Ask attendees to run `podman pull registry.access.redhat.com/ubi9/openjdk-21:1.21` during the break before this lab.

**Key moments to call out:**
- Before starting: "No new extensions in this lab. Quarkus already wrote the Dockerfile for you back when you created the project — we're just going to use it."
- After `quarkus build`: "Open `target/quarkus-app/`. Quarkus doesn't produce one fat JAR — it splits into `lib/` for dependencies and `app/` for your classes. Dependencies change rarely, your code changes constantly."
- Reading `src/main/docker/Dockerfile.jvm`: "Look at the COPY order — `lib/` first, your `app/` classes last. That ordering is deliberate. Rebuild after a code change and only the tiny top layer is rewritten; the 300 MB dependency layer is cached."
- After `podman build`: "You never wrote that Dockerfile. It's been sitting in your project since Lab 1, regenerated and kept current by Quarkus."
- After `podman run`: point at the startup line. "0.8 seconds, inside a container, in prod profile. Same app, no dev mode, no DevServices."
- After `curl /q/health`: "That's the health check you wrote in Lab 3, still working in the container. This is exactly the endpoint a Kubernetes readiness probe would call — the app is deploy-ready even though we're not deploying it today."

**Common questions:**
- *"Why Podman instead of Docker?"* — Podman is daemonless, rootless by default, and fully OCI-compatible. It's a drop-in replacement for the Docker CLI. Red Hat and IBM ship it as the default container engine. Every command in the lab works unchanged with `docker`.
- *"Why not use a container-image extension?"* — You can: `quarkus-container-image-jib` or `-docker` builds the image as part of `quarkus build`. This lab uses the plain Dockerfile so attendees see exactly what's in the image. Mention Jib exists and move on.
- *"Why is the image ~420 MB?"* — That's the UBI9 OpenJDK base, not the app. A native build (`-Dnative`) with a micro base image gets you to ~50 MB — that's the Wrap-Up demo.
- *"Where did the H2 data go when I restarted the container?"* — H2 is in-memory and re-seeded from `import.sql` on every start. A real deployment points at an external database.
- *"How would this get to Kubernetes / OpenShift?"* — The `quarkus-kubernetes` extension generates manifests from `application.properties`; `quarkus-openshift` plus `quarkus deploy` goes straight from laptop to cluster. Both are linked in the lab's "Going further" box. Out of scope today.

**Pitfalls:**
- Port 8080 still held by a `quarkus dev` process from an earlier lab → the `podman run` fails to bind. Have them press `q` in the old terminal or `lsof -i :8080`.
- Forgetting `mvn package` / `quarkus build` before `podman build` → the COPY steps fail because `target/quarkus-app/` doesn't exist. The error is a confusing "no such file or directory" — call it out preemptively.
- Running `podman build` from the wrong directory. The `-f src/main/docker/Dockerfile.jvm` path *and* the trailing `.` build context both assume the project root.
- Apple Silicon: a `SIGILL` on startup means an amd64-only base image. `ubi9/openjdk-21` is multi-arch and correct; `ubi8` is not.
- Podman machine not started on macOS → `Cannot connect to Podman`. Fix with `podman machine start`.

---

### Lab 10 — Quarkus Flow *(optional, ~12 min)*

!!! danger "The most fragile lab in the workshop — read this first"
    Four services, four ports, an OpenAI key, and a multi-step LLM tool chain. Everything that can go wrong in the other labs can go wrong here at once. **Do not have attendees manage four terminals by hand.** Use the provided script:

    ```bash
    export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-...
    bash labs/lab10-quarkus-flow/start-services.sh
    ```

    It starts everything in dependency order, waits for each port, tails all four logs, and tears everything down on a single Ctrl-C. `stop-services.sh` is there if a run is orphaned.

**Pre-flight check:**

- Docker/Podman running — `order-service` starts Kafka DevServices.
- The OpenAI key exported **in the terminal that runs the script**. The script warns if it's missing but starts anyway; `barista-bot` comes up and then refuses every request.
- Ports 8080, 8081, 8082 and 8084 all free. Have everyone press `q` in leftover Dev Mode terminals from Labs 8 and 9 *before* this lab.
- The script needs the **Quarkus CLI** on `PATH` (it runs `quarkus dev`) and uses `lsof` — macOS/Linux only. Windows attendees should follow the manual four-terminal steps in the lab, or pair up.

!!! warning "The silent-skip trap"
    If a port is already in use, `start_service` prints `⚠️ Port 8082 is already in use — assuming order-flow-service is already running, skipping` and carries on. If that "already running" process is actually a stale service from a previous attempt with old code, the lab will misbehave in ways that look like a Quarkus Flow bug. When anything is odd, Ctrl-C, run `stop-services.sh`, and start clean.

**The port map — put this on screen:**

| Port | Service | Comes from |
|------|---------|-----------|
| 8080 | `barista-bot` | `workshop/barista-bot` |
| 8081 | `order-service` | Lab 4 solution |
| 8082 | `order-flow-service` | Lab 10 — the workflow |
| 8084 | `menu-mcp-server` | Lab 8 — same port it has used since Lab 8 |

`menu-mcp-server` uses 8084 from Lab 8 onward, so there is no port move in this lab — it's the same server, the same port, and `barista-bot`'s config already points at `http://localhost:8084/mcp`. If an attendee's bot can't see the menu tools, check those two values first.

**Key moments to call out:**

- Reading the `descriptor()` method: "This is the whole workflow — read it top to bottom like a flowchart. `switchWhenOrElse` is the human-in-the-loop decision point. No state machine class, no BPMN editor, just Java."
- **The two-demo contrast.** This is the lab. Run both back to back in the chat UI:
    - *"I'd like two espressos"* → $5.00 → below the threshold → **CONFIRMED** immediately.
    - *"Actually make it five lattes"* → $21.25 → over the threshold → **PENDING_APPROVAL**, and the bot says it's waiting on the barista.

    Same code path, same prompt, different branch. "The model didn't decide that. The workflow did."
- Open the Admin UI at `http://localhost:8082/admin` on a second screen before you start ordering. It auto-refreshes every 5 seconds, so the pending order appears on its own. Click **Approve** and switch back to the chat — ask "What's the status of my order?" and the bot fetches CONFIRMED. That round trip is the whole point of the lab.
- **Change the threshold live.** Edit `coffee.approval.threshold` in `application.properties` from `15.0` to `3.0`, save, and re-order two espressos. Now $5.00 needs approval. No restart. It's the Lab 3 config lesson landing in a real workflow eight labs later.
- The `barista-bot` log with `log-requests`/`log-responses` on shows the full chain: MCP call for the price, then the `placeOrder` tool, then the workflow response. Scroll through it once — it makes the whole architecture concrete.
- Open `http://localhost:8082/q/dev-ui` and show the Flow panel if time allows.

**Common questions:**

- *"How is this different from just calling the REST endpoint from a tool?"* — For the happy path it isn't. The workflow earns its keep when an order **pauses**: the instance is suspended waiting on an external event, and it resumes when the barista approves — possibly minutes later, from a different process. That's what you'd otherwise hand-roll with a status column and a polling loop.
- *"Where is the pending order stored?"* — In `PendingOrdersStore`, an `@ApplicationScoped` bean holding an in-memory map. Deliberately simple so the lab stays readable. In production this is a database, and Quarkus Flow can persist workflow state properly.
- *"Why does `OrderTools` use snake_case parameter names?"* — The model produces the argument names from the tool schema. The lab uses `item_name` / `quantity` style names because that's what matches reliably; camelCase arguments get mismatched more often.
- *"What is `@Blocking` doing on that endpoint?"* — The Flow API returns a Mutiny `Uni`, so the endpoint runs on an I/O thread. Starting the workflow blocks. Without `@Blocking` you get a `BlockingNotAllowedException`. Good moment to explain Quarkus's reactive/imperative split.
- *"Is Quarkus Flow production-ready?"* — It's a Quarkiverse extension implementing the CNCF Serverless Workflow spec. Treat it as emerging. The concept — declarative, resumable, human-in-the-loop orchestration — is the takeaway, not the specific API.
- *"Why does the chat forget my order after 'End Chat'?"* — That button clears the `ChatMemoryStore` for the session. The order itself is unaffected; it lives in `order-flow-service`.

**Pitfalls:**

- **Key not exported before running the script.** Everything comes up green, then every chat message fails. Check the top of the script output for the warning banner.
- A service fails to start and the script gives up after 120 seconds. The real error is in `labs/lab10-quarkus-flow/logs/<service>.log` — the tailed output scrolls too fast to read live. Open the log file.
- Stale services from a previous run → the silent-skip trap above.
- Attendees who skipped Lab 8 don't have `labs/lab8-mcp-server/menu-mcp-server`, and the script exits with `❌ menu-mcp-server (Lab 8) not found`. It's in the repo, so a `git pull` fixes it — but flag Lab 8 as a hard dependency before anyone starts.
- Order amounts that land near the threshold. $15.00 exactly *does* require approval (the check is "at or above"). Use the two scripted demos rather than improvising prices.
- The LLM occasionally answers "I've placed your order" without actually calling the tool. Confirm against the Admin UI, not the chat text. If it happens, re-ask more explicitly — and use it to make the point that tool calling is probabilistic, which is exactly why the approval gate exists.

---

## Wrap-Up — Native Demo (58:00 – 60:00)

### Pre-workshop: build the native binary

Run this the night before or the morning of the workshop (takes 3–5 minutes):

```bash
cd labs/lab1-rest/solution
quarkus build --native -Dquarkus.native.container-build=true
# Binary output: target/menu-service-1.0.0-SNAPSHOT-runner
```

### Demo script

Open two terminal tabs side by side.

**Tab 1 — JVM mode:**
```bash
cd labs/lab1-rest/solution
time java -jar target/quarkus-app/quarkus-run.jar
# Expected: started in ~0.8s
```

**Tab 2 — Native mode:**
```bash
cd labs/lab1-rest/solution
time ./target/menu-service-1.0.0-SNAPSHOT-runner
# Expected: started in ~0.02s
```

Show the two startup log lines side by side. Let the numbers speak.

**What to say:**
> "Same app. Same code. Same endpoints. The native binary starts 40× faster and uses a fraction of the RAM. This is why Quarkus matters for cloud deployments — smaller containers, faster scaling, lower cost."

---

## Troubleshooting Quick Reference

| Problem | Solution |
|---------|---------|
| Port 8080 in use | `lsof -i :8080` → kill the PID |
| Docker not running | Start Docker Desktop / Podman Desktop |
| Keycloak won't start | Check Docker has at least 2GB RAM allocated |
| OpenAI 401 error | Re-check `QUARKUS_LANGCHAIN4J_OPENAI_API_KEY` is set in the terminal running `quarkus dev` |
| Lab 7 won't start: `SRCFG00014 ... quarkus.langchain4j.openai.api-key is required` | The key variable is missing or misnamed. The usual cause is a participant who already has a plain `OPENAI_API_KEY` exported — Quarkus ignores it. Fix: `export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY="$OPENAI_API_KEY"`. Note the message names the *property*, so people wrongly start editing `application.properties` |
| OpenAI timeout | Network issue at venue — use the recorded fallback demo |
| `@Transactional` missing | Error: `TransactionRequiredException` — add `@Transactional` to the resource method |
| Tests failing on `r` | Check test class has `@QuarkusTest` annotation |
| `podman build` COPY fails | `target/quarkus-app/` is missing — run `quarkus build` / `mvn package` first |
| Podman can't connect (macOS) | `podman machine start` |
| Container exits with `SIGILL` (Apple Silicon) | Base image is amd64-only — use `ubi9/openjdk-21`, not `ubi8` |
| `podman run` can't bind 8080 | A `quarkus dev` process is still running — press `q` in that terminal |
| Bot answers without calling an MCP tool (Lab 8) | `menu-mcp-server` wasn't up when the bot started — restart the bot after the server is listening on 8084 |
| Lab 10 service "already running, skipping" | Stale process on that port — Ctrl-C, run `bash labs/lab10-quarkus-flow/stop-services.sh`, start again |
| Lab 10 service times out after 120s | Read `labs/lab10-quarkus-flow/logs/<service>.log` — the tailed output scrolls past too fast |
| Lab 10 chat fails on every message | OpenAI key wasn't exported in the terminal running `start-services.sh` |
| Native binary missing | Pre-build using the steps in the Wrap-Up section above |
| Import resolution in IDE | Right-click `pom.xml` → "Reload Maven project" (IntelliJ) or reload window (VS Code) |

---

## Catch-Up Instructions for Attendees

If an attendee falls behind, they can jump to any lab's solution directory and continue from there:

=== "Quarkus CLI"

    ```bash
    # Example: jump to Lab 3 solution
    cd labs/lab3-config-health/solution
    quarkus dev
    ```

=== "Maven"

    ```bash
    # Example: jump to Lab 3 solution
    cd labs/lab3-config-health/solution
    mvn quarkus:dev
    ```

All solution projects are standalone Maven projects that run independently.

!!! note "Lab 4 needs two terminals"
    `labs/lab4-kafka/solution` has two separate services (`order-service` and `menu-service`). Start each in its own terminal.

!!! note "Lab 5 — import.sql"
    If starting directly from `labs/lab5-security/solution`, confirm `src/main/resources/import.sql` is present — it seeds the initial menu items.

!!! note "Lab 8 has no `solution/` folder"
    Its two projects sit at the top level: `labs/lab8-mcp-server/menu-mcp-server` and `labs/lab8-mcp-server/barista-bot`. Start the MCP server first.

!!! note "Lab 10 — use the script, not four terminals"
    From the repo root, with the OpenAI key exported:
    ```bash
    bash labs/lab10-quarkus-flow/start-services.sh
    ```
    It runs `setup.sh` automatically if `workshop/barista-bot` doesn't exist yet, starts all four services in order, and stops them all on Ctrl-C.
