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
| 58:00 – 68:00 | Lab 9 | **Requires Docker/Podman + local Kubernetes.** Confirm cluster is ready before starting. The `quarkus build` with Jib takes ~30s. |
| 68:00 – 70:00 | Wrap-Up | Native demo only. Don't try to compile live. Show pre-built binary only. |

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

### Lab 9 — Containerize & Deploy to Kubernetes (58:00 – 68:00)

!!! warning "Pre-flight check"
    Podman Desktop must be running. Attendees need a local Kubernetes cluster — Podman Desktop's built-in Kind cluster is the recommended option. Confirm everyone can run `kubectl get nodes` successfully before starting.

**Key moments to call out:**
- After adding extensions: "Two extensions — `container-image-jib` and `kubernetes`. Jib builds container images in pure Java. No Dockerfile. No container daemon for the build itself."
- After `quarkus build`: "One command built your JAR, built a container image with optimised layers, AND generated Kubernetes manifests. Open `target/kubernetes/kubernetes.yml` — Quarkus wrote that for you."
- After `kubectl apply`: "Notice the pod goes 0/1 then 1/1. That transition is your readiness probe from Lab 3 — Kubernetes is using your health check to decide when to route traffic."
- After scaling to 3 replicas: "Three pods, all healthy, all load-balanced. This is why fast startup matters — each new replica is ready in under a second."

**Common questions:**
- *"Why Jib instead of a Dockerfile?"* — Jib builds optimised layered images without a container daemon. It separates dependencies from application classes, so rebuilds only transfer the changed layers. You can still use Dockerfiles with `quarkus-container-image-docker` if you prefer.
- *"Why Podman instead of Docker?"* — Podman is daemonless, rootless by default, and fully OCI-compatible. It's a drop-in replacement for Docker CLI. Red Hat and IBM ship it as the default container engine.
- *"What about OpenShift?"* — Change `deployment-target=openshift` and Quarkus generates `DeploymentConfig` + `Route` instead of `Deployment` + `Service`.
- *"Is H2 suitable for Kubernetes?"* — No, H2 is in-memory and per-pod. In production you'd use PostgreSQL with a persistent volume. DevServices handles the dev/test story; this lab focuses on the deployment pipeline.

**Pitfalls:**
- No local Kubernetes cluster — guide attendees to create one in Podman Desktop: Settings → Kubernetes → Create Kind cluster.
- Minikube users must run `eval $(minikube podman-env)` first, otherwise the image won't be visible to the cluster.
- Kind users must `kind load docker-image` after building — Kind can't see local Podman images directly.
- NodePort 30080 conflict — unlikely but check with `lsof -i :30080`.

---

## Wrap-Up — Native Demo (68:00 – 70:00)

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
| OpenAI timeout | Network issue at venue — use the recorded fallback demo |
| `@Transactional` missing | Error: `TransactionRequiredException` — add `@Transactional` to the resource method |
| Tests failing on `r` | Check test class has `@QuarkusTest` annotation |
| No local K8s cluster | Podman Desktop: Settings → Kubernetes → Create Kind cluster |
| Image not found by K8s | Minikube: run `eval $(minikube podman-env)` first. Kind: run `kind load docker-image` after build |
| NodePort 30080 in use | `lsof -i :30080` → kill the PID, or change `node-port` in `application.properties` |
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
