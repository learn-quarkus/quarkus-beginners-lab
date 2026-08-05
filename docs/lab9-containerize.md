# Lab 9: Containerization

**Duration:** ~10 minutes &nbsp;|&nbsp; **Project:** `menu-service` (from Lab 3)

!!! info "What you'll build"
    Package `menu-service` into a container image using Quarkus' built-in `Dockerfile`, build it with **Podman** (or Docker), run it locally, and test the REST endpoint — no Kubernetes, no registry, no extra extensions.

!!! warning "Prerequisites for this lab"
    - **Lab 3** completed — this lab uses the `menu-service` you built there (REST + Panache + health checks)
    - **Podman** or **Docker** must be running. Verify with:
    ```bash
    podman info   # or: docker info
    ```

!!! tip "Don't have your Lab 3 menu-service?"
    Run the setup script from the repo root — it copies the Lab 3 solution into a fresh `menu-service` directory:
    ```bash
    bash labs/lab9-containerize/setup.sh
    ```

---

## Step 1 — Build a production JAR

Navigate to your `menu-service` directory from Lab 3:

```bash
cd menu-service
```

Stop Dev Mode if it's still running (`q` in the terminal). Then build the application:

=== "Quarkus CLI"

    ```bash
    quarkus build
    ```

=== "Maven"

    ```bash
    mvn package
    ```

This compiles the app and produces `target/quarkus-app/` — the fast-jar layout Quarkus uses by default.

!!! note "What's in `target/quarkus-app/`?"
    Quarkus splits the JAR into layers so container rebuilds are fast:

    ```
    target/quarkus-app/
    ├── quarkus-run.jar        ← thin launcher
    ├── lib/                   ← all dependencies (changes rarely)
    └── app/                   ← your classes (changes on every build)
    ```

    The `Dockerfile` copies these layers separately so Docker/Podman can cache the `lib/` layer between builds.

---

## Step 2 — Look at the generated Dockerfile

Quarkus generated a `Dockerfile` for you at project creation time. Take a look:

```bash
cat src/main/docker/Dockerfile.jvm
```

You'll see a multi-stage-friendly JVM image based on Red Hat's UBI minimal:

```dockerfile
FROM registry.access.redhat.com/ubi8/openjdk-21:1.20

ENV LANGUAGE='en_US:en'

COPY --chown=185 target/quarkus-app/lib/ /deployments/lib/
COPY --chown=185 target/quarkus-app/*.jar /deployments/
COPY --chown=185 target/quarkus-app/app/ /deployments/app/
COPY --chown=185 target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 \
    -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
```

!!! note "What just happened?"
    Quarkus generates this Dockerfile automatically — you don't write or maintain it. The layered copy order (lib → jar → app → quarkus) means only your changed classes are re-uploaded on rebuild.

---

## Step 3 — Build the container image

=== "Podman"

    ```bash
    podman build -f src/main/docker/Dockerfile.jvm \
      -t menu-service:1.0 .
    ```

=== "Docker"

    ```bash
    docker build -f src/main/docker/Dockerfile.jvm \
      -t menu-service:1.0 .
    ```

You'll see each layer pulled and cached. The final line will read something like:

```
Successfully tagged localhost/menu-service:1.0
```

Verify the image is there:

=== "Podman"

    ```bash
    podman images menu-service
    ```

=== "Docker"

    ```bash
    docker images menu-service
    ```

```
REPOSITORY     TAG   IMAGE ID       CREATED          SIZE
menu-service   1.0   a3b2c1d4e5f6   10 seconds ago   ~420MB
```

---

## Step 4 — Run the container

=== "Podman"

    ```bash
    podman run --rm -p 8080:8080 \
      -e JAVA_OPTS_APPEND="-Dquarkus.profile=prod" \
      menu-service:1.0
    ```

=== "Docker"

    ```bash
    docker run --rm -p 8080:8080 \
      -e JAVA_OPTS_APPEND="-Dquarkus.profile=prod" \
      menu-service:1.0
    ```

Watch the startup log — notice how fast Quarkus starts:

```
INFO  [io.quarkus] menu-service 1.0.0-SNAPSHOT on JVM started in 0.8s.
INFO  [io.quarkus] Profile prod activated.
INFO  [io.quarkus] Installed features: [cdi, hibernate-orm, jdbc-h2, rest, ...]
```

!!! tip "Why `--rm`?"
    `--rm` removes the container automatically when you stop it (`Ctrl+C`). Clean by default — no leftover stopped containers to tidy up.

---

## Step 5 — Test the running container

Open a **second terminal** and hit the endpoints:

```bash
# List all menu items
curl http://localhost:8080/menu

# Check health
curl http://localhost:8080/q/health
```

Expected responses:

```json
[
  {"id":1,"name":"Espresso","description":"A concentrated shot of coffee","price":2.5},
  {"id":2,"name":"Cappuccino","description":"Espresso with steamed milk foam","price":3.75},
  {"id":3,"name":"Cold Brew","description":"12-hour cold-steeped coffee","price":4.0}
]
```

```json
{
  "status": "UP",
  "checks": [
    {"name": "coffee-menu", "status": "UP", "data": {"itemCount": 3}},
    {"name": "Database connections health check", "status": "UP"}
  ]
}
```

Stop the container with `Ctrl+C` in the first terminal when you're done.

!!! warning "H2 is an in-memory database"
    `menu-service` uses H2 in-memory mode. Data is seeded from `import.sql` each time the container starts — any items you added via the API are gone when the container stops. A production deployment would use a persistent external database.

---

## Summary

| What | How |
|------|-----|
| ✅ Production JAR | `quarkus build` / `mvn package` |
| ✅ Container image | `podman build -f src/main/docker/Dockerfile.jvm` |
| ✅ Run locally | `podman run --rm -p 8080:8080` |
| ✅ Health checks work in container | `/q/health` responds `UP` |

!!! tip "Stuck or fell behind?"
    The complete solution is in `labs/lab9-containerize/solution/`. Build and run it with:

    ```bash
    cd labs/lab9-containerize/solution
    mvn package
    podman build -f src/main/docker/Dockerfile.jvm -t menu-service:1.0 .
    podman run --rm -p 8080:8080 menu-service:1.0
    ```

!!! tip "Going further"
    - **Native image:** `mvn package -Dnative -Dquarkus.native.container-build=true` — builds a native binary inside a container, produces a ~50 MB image with ~10ms startup
    - **Push to a registry:** `podman push menu-service:1.0 quay.io/youruser/menu-service:1.0`
    - **Kubernetes deployment:** If you want to go further with K8s, the [`quarkus-kubernetes` extension](https://quarkus.io/guides/deploying-to-kubernetes){ target="_blank" } generates manifests automatically from `application.properties`
    - **OpenShift:** `quarkus deploy` with the [`quarkus-openshift` extension](https://quarkus.io/guides/deploying-to-openshift){ target="_blank" } deploys directly from your laptop

---

[← Lab 8: MCP Server](lab8-mcp-server.md){ .md-button }
[→ Wrap-Up](wrap-up.md){ .md-button .md-button--primary }
