# Lab 3: Config Profiles, Health Checks & Dev UI

**Duration:** 6 minutes &nbsp;|&nbsp; **Project:** `menu-service` (continued from Lab 2)

!!! info "What you'll build"
    Add environment-aware configuration with `@ConfigProperty` and config profiles, a custom liveness health check, and take a guided tour of the Quarkus Dev UI — one of the most powerful developer experience features in the ecosystem.

**New extension added in this lab:**

| Extension | Purpose |
|-----------|---------|
| `quarkus-smallrye-health` | `/q/health` endpoints for liveness and readiness checks |

---

!!! tip "Working directory"
    All commands in this lab run from the `workshop/` folder inside the cloned repo. Make sure you are in that folder before you begin.

## Step 1 — Add the Health Extension

In a second terminal inside `menu-service`:

=== "Quarkus CLI"

    ```bash
    quarkus ext add smallrye-health
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:add-extension -Dextensions="smallrye-health"
    ```

That's it. Visit `http://localhost:8080/q/health` immediately — without writing a single line of code:

```json
{
    "status": "UP",
    "checks": [
        {
            "name": "Database connections health check",
            "status": "UP",
            "data": {
                "<default>": "UP"
            }
        }
    ]
}
```

!!! note "What just happened?"
    `quarkus-smallrye-health` auto-registers a **readiness check** for every configured datasource. Your H2 database is already being checked with no code required.

    Three endpoints are available:

    | Endpoint | Purpose |
    |----------|---------|
    | `/q/health` | All checks combined |
    | `/q/health/live` | Liveness — is the app running? |
    | `/q/health/ready` | Readiness — is the app ready to serve traffic? |

---

## Step 2 — Inject a Config Property

Open `src/main/resources/application.properties` and add:

```properties title="application.properties"
# Shop name — used in the /menu/info endpoint
coffee.shop.name=The Quarkus Cafe
```

Now update `MenuResource.java` to inject and use it.

**1. Add the import** at the top of the file, with the other imports:

```java
import org.eclipse.microprofile.config.inject.ConfigProperty;
```

**2. Add the field and method** inside the `MenuResource` class body:

```java
@ConfigProperty(name = "coffee.shop.name", defaultValue = "The Quarkus Cafe") // (1)
String shopName;

@GET
@Path("/info")
@Produces(MediaType.TEXT_PLAIN) // (2)
public String info() {
    return "Welcome to " + shopName + "! We have " + MenuItem.count() + " items on the menu.";
}
```

1. `@ConfigProperty` injects the value from `application.properties`. If the key is missing, `defaultValue` is used as a fallback.
2. This endpoint returns plain text, not JSON.

Save and visit `http://localhost:8080/menu/info`:

```
Welcome to The Quarkus Cafe! We have 3 items on the menu.
```

!!! note "What just happened?"
    `@ConfigProperty` is MicroProfile Config — a standard, portable API. Quarkus reads it from `application.properties`, environment variables, system properties, and more — in a defined priority order.

    Other useful forms:
    ```java
    // Optional value — no defaultValue needed
    @ConfigProperty(name = "feature.flag")
    Optional<Boolean> featureFlag;

    // Inject a list
    @ConfigProperty(name = "allowed.origins")
    List<String> allowedOrigins;
    ```

---

## Step 3 — Config Profiles

Quarkus has three built-in config profiles: `dev`, `test`, and `prod`. You can override any property per-profile using a `%profile.` prefix.

Add profile-specific shop names to `application.properties`:

```properties title="application.properties"
# Default (used if no profile matches)
coffee.shop.name=The Quarkus Cafe

# Overrides for specific profiles
%dev.coffee.shop.name=The Quarkus Cafe (Dev Mode)
%prod.coffee.shop.name=Production Coffee Co
```

Save the file. Visit `http://localhost:8080/menu/info` again:

```
Welcome to The Quarkus Cafe (Dev Mode)! We have 3 items on the menu.
```

The `%dev.` value is active because you're running in Dev Mode.

!!! tip "How to activate profiles"
    - `dev` — active automatically when you run `quarkus dev`
    - `test` — active automatically during `@QuarkusTest` runs
    - `prod` — active when you run `java -jar target/quarkus-app/quarkus-run.jar`
    - Custom profile — `quarkus dev -Dquarkus.profile=staging`

    You can also create your own profiles. Any `%myprofile.key=value` entry is only active when that profile is selected.

---

## Step 4 — Custom Liveness Health Check

Let's add a meaningful health check: the coffee shop is only healthy if there's at least one item on the menu.

Create `src/main/java/org/coffee/CoffeeShopHealthCheck.java`:

```bash
touch src/main/java/org/coffee/CoffeeShopHealthCheck.java
```

Open `CoffeeShopHealthCheck.java` in your IDE and paste in the following:

```java
package org.coffee;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness // (1)
@ApplicationScoped
public class CoffeeShopHealthCheck implements HealthCheck { // (2)

    @Override
    @Transactional // (3)
    public HealthCheckResponse call() {
        long count = MenuItem.count();
        if (count > 0) {
            return HealthCheckResponse.named("coffee-menu") // (4)
                .up()
                .withData("itemCount", count)
                .build();
        } else {
            return HealthCheckResponse.named("coffee-menu")
                .down()
                .withData("reason", "Menu is empty — no items loaded")
                .build();
        }
    }
}
```

1. `@Liveness` — registers this bean as a liveness check, available at `/q/health/live`. Use `@Readiness` for readiness checks.
2. Implement `HealthCheck` and override `call()` — return UP or DOWN with optional data.
3. `@Transactional` — activates a transaction context so Panache can run the `count()` query. 
4. `HealthCheckResponse.named("coffee-menu")` names the check in the JSON output.

Save and visit `http://localhost:8080/q/health/live`:

```json
{
  "status": "UP",
  "checks": [
    {
      "name": "coffee-menu",
      "status": "UP",
      "data": {
        "itemCount": 3
      }
    }
  ]
}
```

!!! note "What just happened?"
    Quarkus discovers your `@Liveness` bean automatically — no registration, no XML, no `@Bean` factory. CDI and SmallRye Health wire everything together at build time.

---

## Step 5 — Dev UI Guided Tour

The Quarkus Dev UI is a browser-based dashboard that gives you live insight into every aspect of your running application. It's only available in Dev Mode and is stripped completely from production builds.

Open:

```
http://localhost:8080/q/dev-ui
```

Here's what to explore:

**Extensions card**
Shows every extension active in your project with links to their guides. Click any extension name to open its documentation.

**Configuration card → click "Config Editor"**
A live editor for all your `application.properties` values. Change `coffee.shop.name` here and hit `GET /menu/info` — the value updates without a restart. Changes made here are written back to `application.properties`.

**Hibernate ORM card → click "Entity Types"**
Shows your `MenuItem` entity, its mapped table name, and all columns. Click the table name to open a live SQL query browser against your H2 database.

**SmallRye OpenAPI card**
Links directly to your Swagger UI at `/q/swagger-ui` and the raw OpenAPI spec at `/q/openapi`.

**Continuous Testing card**
Shows the status of your last test run. Click **"Run all tests"** or press `r` in the terminal.

!!! tip "Dev UI is dev-only"
    The Dev UI servlet is conditionally included at build time. When you build with `quarkus build` (no `dev` flag), the entire Dev UI is absent from the output JAR — zero overhead in production.

---

## Summary

| What | How |
|------|-----|
| ✅ Injected config | `@ConfigProperty(name = "coffee.shop.name")` |
| ✅ Profile overrides | `%dev.coffee.shop.name=...` in `application.properties` |
| ✅ Free health checks | Added `smallrye-health` — datasource check auto-registered |
| ✅ Custom liveness check | `@Liveness` + `implements HealthCheck` |
| ✅ Dev UI tour | Config editor, entity browser, test panel |

!!! tip "Stuck or fell behind?"
    The complete solution is in `labs/lab3-config-health/solution/`. Run it with:

    === "Quarkus CLI"
        ```bash
        cd labs/lab3-config-health/solution
        quarkus dev
        ```
    === "Maven"
        ```bash
        cd labs/lab3-config-health/solution
        mvn quarkus:dev
        ```

---

[← Lab 2: Panache ORM](lab2-panache.md){ .md-button }
[→ Lab 4: Kafka Messaging](lab4-kafka.md){ .md-button .md-button--primary }
