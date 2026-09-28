# Lab 9: Containerization

**Duration:** ~10 minutes &nbsp;|&nbsp; **Project:** `menu-service`

!!! info "What you'll build"
    Package `menu-service` into a container image using Quarkus' built-in `Dockerfile`, build it with **Podman** (or Docker), run it locally, and test the REST endpoint — no Kubernetes, no registry, no extra extensions.

!!! warning "Prerequisites for this lab"
    - **Podman** or **Docker** must be running. Verify with:
    ```bash
    podman info   # or: docker info
    ```

---

!!! tip "Working directory"
    Unlike the earlier labs, Step 1 here is run from the **repo root** — the setup script lives
    under `labs/`. It then drops you into `workshop/lab9-menu-service`, and every command from
    Step 2 onwards runs from there.

## Step 1 — Get the starting point

Run the setup script from the repo root:

```bash
bash labs/lab9-containerize/setup.sh
cd workshop/lab9-menu-service
```

This creates `workshop/lab9-menu-service` with everything needed for this lab.

!!! note "Note the `workshop/` prefix"
    The script always writes to `workshop/lab9-menu-service` relative to the repo root,
    regardless of which directory you invoke it from — it resolves the repo root from its own
    location. So `cd workshop/lab9-menu-service`, not `cd lab9-menu-service`.

    If the directory already exists the script leaves it untouched and tells you so. To start
    over, delete it first:

    ```bash
    rm -rf workshop/lab9-menu-service
    ```

---

## Step 2 — Build a production JAR

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

## Step 3 — Look at the generated Dockerfile

Quarkus generated a `Dockerfile` for you at project creation time. Take a look:

```bash
cat src/main/docker/Dockerfile.jvm
```

You'll see a multi-stage-friendly JVM image based on Red Hat's UBI minimal:

```dockerfile
FROM registry.access.redhat.com/ubi9/openjdk-21:1.21

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

!!! tip "Apple Silicon (arm64) users"
    The `ubi9/openjdk-21` base image is **multi-arch** — a fresh `podman pull` on Apple Silicon
    gets you the native `arm64` layer automatically, and that's what you want: it starts in
    roughly a third the time of the emulated `amd64` variant.

!!! warning "`SIGILL` on startup under Podman on Apple Silicon"
    On some Podman machine configurations the native `arm64` JDK crashes immediately with:

    ```
    # A fatal error has been detected by the Java Runtime Environment:
    #  SIGILL (0x4) at pc=0x0000ffff96f3fb5c, pid=1, tid=99
    # Problematic frame:
    # j  java.lang.System.registerNatives()V+0 java.base@21.0.6
    ```

    This is the JVM mis-detecting the SVE vector extensions the virtualised CPU reports.
    It is **not** a problem with your build. Disable SVE detection when you run:

    ```bash
    podman run --rm -p 8080:8080 \
      -e JAVA_OPTS_APPEND="-Dquarkus.profile=prod -XX:UseSVE=0" \
      menu-service:1.0
    ```

    If you'd rather not chase JVM flags, forcing the emulated image also works —
    `podman build --platform linux/amd64 ...` — at the cost of a slower start.

---

## Step 4 — Build the container image

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
REPOSITORY              TAG   IMAGE ID       CREATED          SIZE
localhost/menu-service  1.0   679c54ee5037   10 seconds ago   480 MB
```

!!! note "Why `localhost/menu-service` and not just `menu-service`?"
    Podman always records a fully-qualified image name. Because you built with `-t menu-service:1.0`
    and gave no registry, it assumes the local one and stores the image as
    `localhost/menu-service`. Docker would show plain `menu-service`.

    You can still refer to it as `menu-service:1.0` in `podman run` — the short name resolves.
    The image is around **470–480 MB**: a JVM base layer plus your application. The native-image
    build mentioned at the end of this lab is what gets you down to ~50 MB.

---

## Step 5 — Run the container

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
INFO  [io.quarkus] (main) menu-service 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.39.2) started in 0.857s. Listening on: http://0.0.0.0:8080
INFO  [io.quarkus] (main) Profile prod activated.
INFO  [io.quarkus] (main) Installed features: [agroal, cdi, hibernate-orm, hibernate-orm-panache, jdbc-h2, narayana-jta, rest, rest-jackson, smallrye-context-propagation, smallrye-health, smallrye-openapi, vertx]
```

Expect somewhere between **under a second and about three seconds**. A container running on your
CPU's native architecture starts in well under a second even though it has to boot a fresh JVM,
initialise H2 and run `import.sql`; an emulated image (an `amd64` image on Apple Silicon, say) is
two to three times slower. Either way it's a fraction of a traditional application server's startup.

!!! tip "Why `--rm`?"
    `--rm` removes the container automatically when you stop it (`Ctrl+C`). Clean by default — no leftover stopped containers to tidy up.

---

## Step 6 — Test the running container

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
  {"id":51,"name":"Cappuccino","description":"Espresso with steamed milk foam","price":3.75},
  {"id":101,"name":"Cold Brew","description":"12-hour cold-steeped coffee","price":4.0}
]
```

!!! note "Why `1`, `51`, `101` and not `1`, `2`, `3`?"
    Hibernate's default sequence generator allocates IDs in blocks of **50** (`allocationSize=50`)
    so it doesn't have to hit the database on every insert. Each row in `import.sql` lands at the
    start of a new block. The gaps are expected and harmless — IDs are identifiers, not a count.

```json
{
  "status": "UP",
  "checks": [
    {"name": "coffee-menu", "status": "UP", "data": {"itemCount": 3}},
    {"name": "Database connections health check", "status": "UP", "data": {"<default>": "UP"}}
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
    bash labs/lab9-containerize/setup.sh
    cd workshop/lab9-menu-service
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
[→ Lab 10: Quarkus Flow](lab10-quarkus-flow.md){ .md-button .md-button--primary }
