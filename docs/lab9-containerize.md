# Lab 9: Containerize & Deploy to Kubernetes

**Duration:** ~15 minutes &nbsp;|&nbsp; **Project:** `menu-service` (continued)

!!! info "What you'll build"
    Build a container image of `menu-service` using **Jib** (no Dockerfile needed), generate **Kubernetes manifests** automatically, and deploy to a local Kubernetes cluster — all driven by Quarkus extensions and a single `quarkus build` command.

**New extensions added in this lab:**

| Extension | Purpose |
|-----------|---------|
| `quarkus-container-image-jib` | Builds OCI container images using Jib — no Docker daemon or Dockerfile required |
| `quarkus-kubernetes` | Generates Kubernetes `Deployment` + `Service` YAML from your config |
| `quarkus-smallrye-health` | Already present from Lab 3 — Kubernetes uses the health endpoints for probes |

!!! warning "Prerequisites for this lab"
    - **Podman Desktop** or **Docker Desktop** must be running (for building and hosting container images)
    - **A local Kubernetes cluster** — Podman Desktop and Docker Desktop both include a built-in Kind/Kubernetes provider, or you can use Minikube. See the setup section below if you don't have one yet.

---

## Setup — Local Kubernetes Cluster

If you already have `kubectl` working against a local cluster, skip to [Step 1](#step-1-add-the-extensions).

=== "Podman Desktop"

    Podman Desktop can create a local Kubernetes cluster for you using its built-in Kind provider:

    1. Open **Podman Desktop** → **Settings** → **Kubernetes**
    2. Click **Create Kind cluster** → name it `quarkus-workshop` → click **Create**
    3. Wait ~1 minute for the cluster to start
    4. Podman Desktop automatically configures `kubectl` to point at the new cluster

    Alternatively, from the terminal:

    ```bash
    kind create cluster --name quarkus-workshop
    ```

    !!! note "Kind and local images"
        Kind runs inside Podman. After building your image, you'll need to load it into the cluster:
        ```bash
        kind load docker-image quarkus-workshop/menu-service:1.0.0-SNAPSHOT \
          --name quarkus-workshop
        ```

=== "Docker Desktop"

    Docker Desktop ships with a built-in single-node Kubernetes cluster:

    1. Open **Docker Desktop** → **Settings** → **Kubernetes**
    2. Check **Enable Kubernetes** → click **Apply & Restart**
    3. Wait ~2 minutes for the cluster to start
    4. Docker Desktop automatically configures `kubectl` to point at the new cluster

    To switch to the Docker Desktop context if you have other clusters:

    ```bash
    kubectl config use-context docker-desktop
    ```

    !!! note "Docker Desktop and local images"
        Images built by Jib are pushed directly into Docker Desktop's local registry — no manual load step needed.

=== "Minikube"

    ```bash
    # Install (macOS)
    brew install minikube

    # Start a cluster using the Podman driver
    minikube start --driver=podman

    # Point your shell to Minikube's daemon
    eval $(minikube podman-env)
    ```

    !!! tip "Why `minikube podman-env`?"
        This makes Jib push images directly into Minikube's container runtime — no need for a registry.

Verify your cluster is ready:

```bash
kubectl get nodes
```

```
NAME                              STATUS   ROLES           AGE   VERSION
quarkus-workshop-control-plane    Ready    control-plane   1m    v1.31.0
```

---

## Step 1 — Add the Extensions

Stop Dev Mode if it's still running (`q` in the terminal). Then add the extensions:

=== "Quarkus CLI"

    ```bash
    quarkus ext add container-image-jib,kubernetes
    ```

=== "Maven"

    ```bash
    mvn quarkus:add-extension -Dextensions="container-image-jib,kubernetes"
    ```

Open `pom.xml` and confirm two new dependencies appeared:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-container-image-jib</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-kubernetes</artifactId>
</dependency>
```

!!! note "What just happened?"
    - **Jib** builds container images in pure Java — no container daemon needed for the build itself. It layers your app efficiently (dependencies, resources, classes) for fast rebuilds.
    - **`quarkus-kubernetes`** generates `Deployment` and `Service` YAML at build time by reading your Quarkus config. No hand-written manifests.

---

## Step 2 — Configure the Container Image

Open `src/main/resources/application.properties` and add the container image configuration:

```properties title="application.properties"
# Container image — built by Jib
quarkus.container-image.builder=jib
quarkus.container-image.group=quarkus-workshop
quarkus.container-image.name=menu-service
quarkus.container-image.tag=1.0.0-SNAPSHOT
```

!!! note "What these properties do"
    | Property | Value | Effect |
    |----------|-------|--------|
    | `builder` | `jib` | Use Jib instead of a container daemon build |
    | `group` | `quarkus-workshop` | Image namespace (like a Docker Hub org) |
    | `name` | `menu-service` | Image name |
    | `tag` | `1.0.0-SNAPSHOT` | Image tag |

    The final image name will be: `quarkus-workshop/menu-service:1.0.0-SNAPSHOT`

---

## Step 3 — Configure Kubernetes Manifests

Add the Kubernetes configuration to the same `application.properties`:

```properties title="application.properties"
# Kubernetes deployment configuration
quarkus.kubernetes.deployment-target=kubernetes
quarkus.kubernetes.replicas=1

# Use the local image — don't try to pull from a remote registry
quarkus.kubernetes.image-pull-policy=IfNotPresent

# Expose the service outside the cluster
quarkus.kubernetes.service-type=NodePort
quarkus.kubernetes.ports.http.node-port=30080
```

!!! note "What these properties do"
    | Property | Effect |
    |----------|--------|
    | `deployment-target=kubernetes` | Generate plain Kubernetes YAML (alternatives: `openshift`, `knative`) |
    | `replicas=1` | One pod — suitable for a workshop |
    | `image-pull-policy=IfNotPresent` | Use the local image; don't pull from a remote registry |
    | `service-type=NodePort` | Expose the service on a fixed port on every cluster node |
    | `node-port=30080` | Access the service at `http://localhost:30080` |

!!! warning "Kind doesn't expose NodePorts directly"
    If you're using Kind (via Podman Desktop or the CLI), `NodePort` services are not reachable at `localhost:30080` because Kind nodes run inside a container network. Use `kubectl port-forward` instead — shown in Step 8.

---

## Step 4 — Configure Health Probes

Kubernetes uses **liveness** and **readiness** probes to know when your pod is healthy. Quarkus auto-configures these from your health extension (Lab 3).

Add to `application.properties`:

```properties title="application.properties"
# Health probes — Kubernetes reads these automatically
quarkus.kubernetes.liveness-probe.http-action-path=/q/health/live
quarkus.kubernetes.readiness-probe.http-action-path=/q/health/ready
quarkus.kubernetes.liveness-probe.initial-delay=5s
quarkus.kubernetes.readiness-probe.initial-delay=5s
```

!!! tip "Free health probes"
    Because you already added `quarkus-smallrye-health` in Lab 3, Kubernetes will check `/q/health/live` and `/q/health/ready` automatically. Your custom `CoffeeShopHealthCheck` from Lab 3 is included — if the menu is empty, Kubernetes will detect it.

---

## Step 5 — Build the Container Image

Build the application **and** the container image in one command:

```bash
quarkus build -Dquarkus.container-image.build=true
```

Or with Maven:

```bash
mvn package -Dquarkus.container-image.build=true
```

You'll see output like:

```
[INFO] [io.quarkus.container.image.jib.deployment.JibProcessor]
       Created container image quarkus-workshop/menu-service:1.0.0-SNAPSHOT
```

!!! note "What just happened?"
    Jib built a container image with three optimised layers:
    
    1. **Dependencies** — changes rarely, cached across builds
    2. **Resources** — `application.properties`, `import.sql`
    3. **Application classes** — changes on every build, smallest layer
    
    This layering means rebuilds after code changes only transfer a few KB — not the entire JAR.

Verify the image exists:

```bash
podman images | grep menu-service
```

```
quarkus-workshop/menu-service   1.0.0-SNAPSHOT   abc123def456   10 seconds ago   423MB
```

!!! tip "Docker CLI works too"
    If you have the `podman-docker` compatibility package installed, `docker images` works identically — Podman is a drop-in replacement.

---

## Step 6 — Inspect the Generated Kubernetes Manifests

Quarkus generated Kubernetes YAML at build time. Take a look:

```bash
cat target/kubernetes/kubernetes.yml
```

You'll see a complete `Deployment` and `Service` — generated entirely from your `application.properties`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: menu-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: menu-service
  template:
    metadata:
      labels:
        app.kubernetes.io/name: menu-service
    spec:
      containers:
        - name: menu-service
          image: quarkus-workshop/menu-service:1.0.0-SNAPSHOT
          ports:
            - containerPort: 8080
              name: http
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: 8080
            initialDelaySeconds: 5
          readinessProbe:
            httpGet:
              path: /q/health/ready
              port: 8080
            initialDelaySeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: menu-service
spec:
  type: NodePort
  ports:
    - port: 8080
      targetPort: 8080
      nodePort: 30080
      name: http
```

!!! note "What just happened?"
    You didn't write any YAML. Quarkus read your `application.properties`, your health extension, and your container image config — and generated production-ready Kubernetes manifests with correct probes, ports, and image references.

---

## Step 7 — Deploy to Kubernetes

=== "Podman Desktop / Kind (Recommended)"

    Load the image into Kind first, then deploy:

    ```bash
    kind load docker-image quarkus-workshop/menu-service:1.0.0-SNAPSHOT \
      --name quarkus-workshop

    kubectl apply -f target/kubernetes/kubernetes.yml
    ```

=== "Minikube (with podman-env)"

    The image is already in Minikube's runtime. Deploy directly:

    ```bash
    kubectl apply -f target/kubernetes/kubernetes.yml
    ```

Watch the pod start:

```bash
kubectl get pods -w
```

```
NAME                            READY   STATUS    RESTARTS   AGE
menu-service-7b8f9d6c4f-x2j9k  0/1     Running   0          3s
menu-service-7b8f9d6c4f-x2j9k  1/1     Running   0          8s
```

!!! tip "The pod went from 0/1 to 1/1"
    That transition happened because the **readiness probe** (`/q/health/ready`) returned `UP`. Kubernetes won't route traffic to the pod until it's ready.

---

## Step 8 — Test the Deployed Service

!!! warning "H2 is an in-memory database"
    `menu-service` uses H2 in-memory mode. The `import.sql` seed data is loaded when the pod starts, so the initial response looks correct. However, **any items you add via the REST API are lost when the pod restarts**. This is fine for a workshop demo — a production deployment would use a persistent database (PostgreSQL, MySQL, etc.).

=== "Podman Desktop / Kind"

    ```bash
    # Port-forward since Kind doesn't expose NodePorts directly
    kubectl port-forward svc/menu-service 30080:8080 &
    curl http://localhost:30080/menu
    ```

=== "Docker Desktop"

    ```bash
    curl http://localhost:30080/menu
    ```

=== "Minikube"

    ```bash
    curl $(minikube ip):30080/menu
    ```

You should see the familiar JSON response:

```json
[
  {"id":1,"name":"Espresso","description":"A concentrated shot of coffee","price":2.5},
  {"id":2,"name":"Cappuccino","description":"Espresso with steamed milk foam","price":3.75},
  {"id":3,"name":"Cold Brew","description":"12-hour cold-steeped coffee","price":4.0}
]
```

Check the health endpoint too:

```bash
curl http://localhost:30080/q/health
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

---

## Step 9 — Explore the Deployment

Run a few `kubectl` commands to see what Quarkus deployed:

```bash
# See the deployment
kubectl get deployment menu-service

# See the service and its NodePort
kubectl get svc menu-service

# Check pod logs — notice the fast startup time
kubectl logs deployment/menu-service

# Describe the pod — see the probe configuration
kubectl describe pod -l app.kubernetes.io/name=menu-service
```

In the pod description, find the `Liveness` and `Readiness` sections:

```
Liveness:   http-get http://:8080/q/health/live delay=5s timeout=1s period=10s
Readiness:  http-get http://:8080/q/health/ready delay=5s timeout=1s period=10s
```

!!! note "What just happened?"
    Kubernetes is actively monitoring your application using the health checks you built in Lab 3. If the liveness probe fails, Kubernetes restarts the pod. If the readiness probe fails, Kubernetes stops sending traffic to it.

---

## Bonus — Scale Up and Watch

Scale to 3 replicas and watch Kubernetes bring them up:

```bash
kubectl scale deployment menu-service --replicas=3
kubectl get pods -w
```

```
NAME                            READY   STATUS    RESTARTS   AGE
menu-service-7b8f9d6c4f-x2j9k  1/1     Running   0          2m
menu-service-7b8f9d6c4f-a4m2n  1/1     Running   0          5s
menu-service-7b8f9d6c4f-p7q3r  1/1     Running   0          5s
```

Three pods, all healthy, all responding to the same `NodePort`. Kubernetes load-balances across them automatically.

Scale back down when done:

```bash
kubectl scale deployment menu-service --replicas=1
```

---

## Cleanup

When you're done, remove the deployment from your cluster:

```bash
kubectl delete -f target/kubernetes/kubernetes.yml
```

---

## Summary

| What | How |
|------|-----|
| ✅ Built a container image | `quarkus-container-image-jib` — no Dockerfile needed |
| ✅ Generated Kubernetes YAML | `quarkus-kubernetes` — from `application.properties` |
| ✅ Health probes wired automatically | Lab 3's `@Liveness` + `@Readiness` checks used by K8s |
| ✅ Deployed to a local cluster | `kubectl apply -f target/kubernetes/kubernetes.yml` |
| ✅ Scaled replicas | `kubectl scale` — instant horizontal scaling |

!!! tip "Stuck or fell behind?"
    The complete solution is in `labs/lab9-containerize/solution/`. Build and deploy it with:

    ```bash
    cd labs/lab9-containerize/solution
    mvn package -Dquarkus.container-image.build=true
    kubectl apply -f target/kubernetes/kubernetes.yml
    ```

!!! tip "Going further"
    - **OpenShift:** Change `deployment-target=openshift` to generate `DeploymentConfig` + `Route` manifests
    - **Knative:** Change `deployment-target=knative` for serverless scale-to-zero deployments
    - **Native image:** Add `-Dquarkus.native.enabled=true` to build a native container (~35 MB, ~20ms startup)
    - **Remote registry:** Set `quarkus.container-image.push=true` and `quarkus.container-image.registry=ghcr.io` to push to GitHub Container Registry

---

[← Lab 8: MCP Server](lab8-mcp-server.md){ .md-button }
[→ Wrap-Up](wrap-up.md){ .md-button .md-button--primary }
