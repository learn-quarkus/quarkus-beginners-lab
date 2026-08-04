# Lab 6: Fault Tolerance

**Duration:** 5 minutes &nbsp;|&nbsp; **Project:** `menu-service` (continued)

!!! info "What you'll build"
    Add `@Retry`, `@Fallback`, and `@Timeout` to a `PricingService` that simulates a flaky external pricing API. Three annotations turn an unreliable service call into a resilient one — with no retry loops, no try/catch, no thread management.

**New extension added in this lab:**

| Extension | Purpose |
|-----------|---------|
| `quarkus-smallrye-fault-tolerance` | MicroProfile Fault Tolerance — `@Retry`, `@Fallback`, `@Timeout`, `@CircuitBreaker` |

---

## Background: Why Fault Tolerance?

!!! note "Why fault tolerance?"
    Microservices call each other over the network. Networks fail. External services go down. Without resilience patterns, a single failing downstream call can cascade into a full outage.

    The naive solution is writing retry loops manually:
    ```java
    for (int i = 0; i < 3; i++) {
        try { return externalService.call(); }
        catch (Exception e) { if (i == 2) throw e; }
    }
    ```

    This is noisy, error-prone, and untestable. MicroProfile Fault Tolerance replaces it with a single annotation. Quarkus applies it via CDI interception — no framework boilerplate, no wrapping classes.

---

## Step 1 — Add the Extension

=== "Quarkus CLI"

    ```bash
    quarkus ext add smallrye-fault-tolerance
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:add-extension -Dextensions="smallrye-fault-tolerance"
    ```

Enable debug logging so retries are visible in the terminal. Add to `application.properties`:

```properties title="application.properties"
# Show retry/fallback log lines from our package
quarkus.log.category."org.coffee".level=DEBUG
```

---

## Step 2 — Create the PricingService

Create `src/main/java/org/coffee/PricingService.java`:

```java
package org.coffee;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

@ApplicationScoped // (1)
public class PricingService {

    private static final Logger LOG = Logger.getLogger(PricingService.class);

    @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS) // (2)
    @Fallback(fallbackMethod = "defaultPrice")                          // (3)
    @Timeout(value = 2, unit = ChronoUnit.SECONDS)                     // (4)
    public BigDecimal getPrice(Long itemId) {
        LOG.debugf("Fetching price for item %d ...", itemId);

        // Simulate a flaky external pricing API — fails 50% of the time
        if (Math.random() < 0.5) {
            LOG.debugf("Simulated failure for item %d — will retry", itemId);
            throw new RuntimeException("External pricing service unavailable");
        }

        LOG.debugf("Got price for item %d", itemId);
        return BigDecimal.valueOf(3.50 + (itemId % 3));
    }

    // Called automatically when ALL retries are exhausted
    public BigDecimal defaultPrice(Long itemId) {
        LOG.debugf("Returning fallback price $4.99 for item %d", itemId);
        return BigDecimal.valueOf(4.99);
    }
}
```

1. Must be a CDI bean (`@ApplicationScoped`, `@RequestScoped`, etc.) for the annotations to be intercepted.
2. `@Retry` — if `getPrice()` throws, retry up to 3 times with a 200ms delay between attempts.
3. `@Fallback` — if all retries are exhausted, call `defaultPrice()` instead of propagating the exception.
4. `@Timeout` — if `getPrice()` takes longer than 2 seconds, interrupt it and treat it as a failure.

!!! note "Execution order"
    The annotations wrap each other like layers:
    ```
    Timeout
      └── Retry
            └── getPrice()   ← actual method
                  └── Fallback (last resort)
    ```
    Timeout is the outermost layer — if the total time (including retries) exceeds 2 seconds, it fires. Fallback fires only after all retries are exhausted.

---

## Step 3 — Add the Price Endpoint to MenuResource

Add the following import and method to `MenuResource.java`:

```java
// Add this import at the top:
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;
import java.math.BigDecimal;

// Add this field inside MenuResource:
@Inject
PricingService pricingService;

// Add this method inside MenuResource:
@GET
@Path("/{id}/price")
@Produces(MediaType.TEXT_PLAIN)
public BigDecimal getPrice(@PathParam("id") Long id) {
    return pricingService.getPrice(id);
}
```

Save. Quarkus live-reloads.

---

## Step 4 — Watch It Work

Open `http://localhost:8080/q/swagger-ui` and find `GET /menu/{id}/price`.

Click **Try it out**, enter `id = 1`, and click **Execute** repeatedly — 6 or 7 times.

Watch the `quarkus dev` terminal. You'll see a mix of:

**Successful call (no retry needed):**
```
DEBUG [org.coffee.PricingService] Fetching price for item 1 ...
DEBUG [org.coffee.PricingService] Got price for item 1
```

**Retry scenario:**
```
DEBUG [org.coffee.PricingService] Fetching price for item 1 ...
DEBUG [org.coffee.PricingService] Simulated failure for item 1 — will retry
DEBUG [org.coffee.PricingService] Fetching price for item 1 ...
DEBUG [org.coffee.PricingService] Got price for item 1
```

**Fallback scenario (all 3 retries failed):**
```
DEBUG [org.coffee.PricingService] Simulated failure for item 1 — will retry
DEBUG [org.coffee.PricingService] Simulated failure for item 1 — will retry
DEBUG [org.coffee.PricingService] Simulated failure for item 1 — will retry
DEBUG [org.coffee.PricingService] Returning fallback price $4.99 for item 1
```

Notice: **the endpoint always returns a value** — never a 500 error — even when all retries fail. The user sees `4.99` (the fallback price) instead of an error page.

!!! tip "Circuit Breaker — the natural next step"
    `@CircuitBreaker` builds on `@Retry`. After N consecutive failures, it **opens the circuit** — subsequent calls fail immediately (no retries) for a configurable time window, giving the downstream service time to recover. After the window, it tries one request; if it succeeds, the circuit closes again.

    ```java
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 10000)
    public BigDecimal getPrice(Long itemId) { ... }
    ```

    See the [Quarkus Fault Tolerance guide](https://quarkus.io/guides/smallrye-fault-tolerance) for the full API.

---

## Summary

| What | How |
|------|-----|
| ✅ Automatic retries | `@Retry(maxRetries = 3, delay = 200)` |
| ✅ Graceful fallback | `@Fallback(fallbackMethod = "defaultPrice")` |
| ✅ Timeout protection | `@Timeout(value = 2, unit = ChronoUnit.SECONDS)` |
| ✅ No retry boilerplate | Annotations on a CDI bean method |

!!! tip "Stuck or fell behind?"
    The complete solution is in `labs/lab6-fault-tolerance/solution/`. Run it with:

    === "Quarkus CLI"
        ```bash
        cd labs/lab6-fault-tolerance/solution
        quarkus dev
        ```
    === "Maven"
        ```bash
        cd labs/lab6-fault-tolerance/solution
        ./mvnw quarkus:dev
        ```

---

[← Lab 5: OIDC Security](lab5-security.md){ .md-button }
[→ Lab 7: AI with LangChain4j](lab7-langchain4j.md){ .md-button .md-button--primary }
