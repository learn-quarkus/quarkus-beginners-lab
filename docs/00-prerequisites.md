# Prerequisites

Complete all steps on this page **before** the workshop starts. The setup takes 10–15 minutes.

---

## 1. Java 21+

Quarkus 3.x requires Java 17 minimum; this workshop uses Java 21.

=== "macOS / Linux (SDKMAN)"

    ```bash
    # Install SDKMAN if you don't have it
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"

    # Install Java 21
    sdk install java 21.0.3-tem

    # Verify
    java -version
    ```

=== "macOS (Homebrew)"

    ```bash
    brew install --cask temurin@21
    java -version
    ```

=== "Windows"

    Download and install **Eclipse Temurin 21** from [adoptium.net](https://adoptium.net/temurin/releases/?version=21).

    After installation, verify in PowerShell:
    ```powershell
    java -version
    ```

!!! note "Expected output"
    ```
    openjdk version "21.0.x" ...
    ```

---

## 2. Maven 3.9+ or Quarkus CLI

You only need **one** of these. The Quarkus CLI is recommended — it makes bootstrapping projects much faster.

=== "Quarkus CLI (recommended)"

    ```bash
    # macOS / Linux via SDKMAN
    sdk install quarkus

    # macOS via Homebrew
    brew install quarkusio/tap/quarkus

    # Verify
    quarkus version
    ```

=== "Maven 3.9+"

    ```bash
    # macOS / Linux via SDKMAN
    sdk install maven 3.9.6

    # macOS via Homebrew
    brew install maven

    # Verify
    mvn -version
    ```

---

## 3. Docker Desktop or Podman

!!! info "Required for Labs 4 & 5 only"
    Labs 1, 2, 3, 6, and 7 do **not** need Docker. You only need a container runtime for:

    - **Lab 4** — DevServices auto-starts a Kafka (Redpanda) container
    - **Lab 5** — DevServices auto-starts a Keycloak container

=== "Docker Desktop"

    Download from [docs.docker.com/get-docker](https://docs.docker.com/get-docker/).

    After install, verify:
    ```bash
    docker ps
    ```

=== "Podman Desktop"

    Download from [podman-desktop.io](https://podman-desktop.io/).

    After install, verify:
    ```bash
    podman ps
    ```

!!! warning "Make sure Docker/Podman is running"
    The container daemon must be **running** (not just installed) before you start Labs 4 and 5.
    You should see an empty table, not an error, when you run `docker ps` or `podman ps`.

---

## 4. OpenAI API Key

!!! info "Required for Lab 7 only"
    Labs 1–6 do not require an API key.

!!! tip "Workshop instructor key"
    If you are attending an instructor-led session, your instructor will provide a shared API key for Lab 7. You do not need to create your own OpenAI account.

    If you are working through this workshop independently, you can use your own key from [platform.openai.com](https://platform.openai.com).

You will need to set the key as an environment variable in the terminal where you run `quarkus dev` for Lab 7. This is a **session-only** setting — it is not saved permanently.

=== "macOS / Linux"

    ```bash
    export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-...
    ```

=== "Windows (PowerShell)"

    ```powershell
    $env:QUARKUS_LANGCHAIN4J_OPENAI_API_KEY="sk-..."
    ```

!!! warning "Session only"
    This sets the key for the current terminal session only. It is not written to any file and disappears when you close the terminal — which is exactly what you want for a shared or temporary key.

---

## 5. IDE Setup

Either IDE works well. Install the Quarkus plugin for the best experience.

=== "VS Code"

    1. Install [VS Code](https://code.visualstudio.com/)
    2. Install the **[Quarkus](https://marketplace.visualstudio.com/items?itemName=redhat.vscode-quarkus)** extension (by Red Hat)
    3. Install the **[Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)** extension

=== "IntelliJ IDEA"

    1. Install [IntelliJ IDEA Community or Ultimate](https://www.jetbrains.com/idea/)
    2. The **Quarkus** plugin is bundled in Ultimate; for Community install it from **Preferences → Plugins → Marketplace → search "Quarkus"**

---

## 6. Clone the Workshop Repository

The workshop repo contains the `prereq-check.sh` script and all solution code. Clone it now so it's ready before the session starts.

```bash
git clone https://github.com/learn-quarkus/quarkus-beginners-lab.git
cd quarkus-beginners-lab
```

!!! note "What's in the repo?"
    ```
    quarkus-beginners-lab/
    ├── docs/           ← this website's source
    ├── labs/           ← complete solution code for every lab
    └── prereq-check.sh ← environment validation script
    ```

---

## 7. Verify Your Setup

Run the prerequisite check script from inside the cloned repo:

```bash
chmod +x prereq-check.sh
./prereq-check.sh
```

Or check manually:

```bash
java -version       # should show 21.x
quarkus version     # should show 3.x   (or: mvn -version for Maven)
docker ps           # should show an empty table, not an error
```

!!! info "OpenAI key — Lab 7 only"
    You do not need the API key until Lab 7. Your instructor will provide one at that point, or you can use your own.

!!! success "All green? You're ready!"
    [→ Start Lab 1](lab1-rest.md){ .md-button .md-button--primary }

---

## 8. Serve the Docs Locally (Optional — self-paced only)

!!! info "Instructor-led sessions"
    Your instructor will project the lab guide. Skip this section.

If you are working through this workshop independently, you can run the docs site on your laptop so you can read it offline or in a side-by-side window.

**Install the dependencies** (one-time):

```bash
pip install mkdocs-material pymdown-extensions
```

**Start the local docs server** from inside the cloned repo:

```bash
mkdocs serve
```

Open **`http://127.0.0.1:8000`** in your browser. The site hot-reloads whenever a doc file changes.

!!! tip "Port conflict?"
    If port 8000 clashes with something else, use a different port:
    ```bash
    mkdocs serve --dev-addr 127.0.0.1:8001
    ```
