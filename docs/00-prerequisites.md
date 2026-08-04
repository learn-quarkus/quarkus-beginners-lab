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

1. Sign in or create an account at [platform.openai.com](https://platform.openai.com)
2. Navigate to **API Keys** → **Create new secret key**
3. Copy the key — you will not be able to see it again

Set it in your shell for the workshop session:

=== "macOS / Linux"

    ```bash
    export OPENAI_API_KEY=sk-...
    export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-...
    ```

    To persist across sessions, add those lines to your `~/.zshrc` or `~/.bashrc`.

=== "Windows (PowerShell)"

    ```powershell
    $env:OPENAI_API_KEY="sk-..."
    $env:QUARKUS_LANGCHAIN4J_OPENAI_API_KEY="sk-..."
    ```

Alternatively, create a `.env` file in the `labs/lab7-langchain4j/` directory:

```properties title=".env"
OPENAI_API_KEY=sk-...
QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-...
```

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

## 6. Verify Your Setup

Run the prerequisite check script to confirm everything is ready:

```bash
# From the root of the workshop repo
chmod +x prereq-check.sh
./prereq-check.sh
```

Or check manually:

```bash
java -version       # should show 21.x
quarkus version     # should show 3.x   (or: mvn -version for Maven)
docker ps           # should show an empty table, not an error
echo $OPENAI_API_KEY  # should show sk-...  (needed only for Lab 7)
```

!!! success "All green? You're ready!"
    [→ Start Lab 1](lab1-rest.md){ .md-button .md-button--primary }
