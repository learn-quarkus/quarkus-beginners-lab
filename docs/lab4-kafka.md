# Lab 4: Kafka Messaging with DevServices

**Duration:** 7 minutes &nbsp;|&nbsp; **Projects:** `order-service` (new) + `menu-service` (continued)

!!! info "What you'll build"
    Create a new `order-service` that publishes coffee orders to a Kafka topic. `menu-service` consumes them.
    You will write **zero Kafka infrastructure config** — DevServices auto-starts a Kafka-compatible broker the moment you add the extension.

!!! warning "Docker or Podman required"
    Before starting this lab, confirm your container runtime is running:
    ```bash
    docker ps   # or: podman ps
    ```
    You should see an empty table — not an error. If you see an error, start Docker Desktop or Podman Desktop first.

```
┌─────────────────────┐    Kafka topic      ┌──────────────────────┐
│    order-service    │  "coffee-orders"    │    menu-service      │
│  POST /orders       │ ──────────────────▶ │  @Incoming consumer  │
│  Emitter.send(json) │                     │  logs each order     │
└─────────────────────┘                     └──────────────────────┘
         ↑                                            ↑
   port 8080                                    port 8081
```

**Extensions used:**

| Extension | Purpose |
|-----------|---------|
| `quarkus-messaging-kafka` | SmallRye Reactive Messaging Kafka connector |

---

## Step 1 — Create the order-service Project

Open a **new terminal** (keep `menu-service` running) and bootstrap a second project:

=== "Quarkus CLI"

    ```bash
    quarkus create app org.coffee:order-service \
      --extensions=rest-jackson,smallrye-openapi,messaging-kafka
    cd order-service
    ```

=== "Maven"

    ```bash
    mvn io.quarkus.platform:quarkus-maven-plugin:3.33.3:create \
      -DprojectGroupId=org.coffee \
      -DprojectArtifactId=order-service \
      -Dextensions=rest-jackson,smallrye-openapi,messaging-kafka
    cd order-service
    ```

!!! note "What just happened?"
    You created a second standalone Quarkus project. It will run on port **8081** — you'll configure this explicitly in Step 4 so it doesn't clash with `menu-service` on 8080.

!!! tip "Delete the generated sample files"
    ```bash
    rm src/main/java/org/coffee/GreetingResource.java
    rm src/test/java/org/coffee/GreetingResourceTest.java
    rm src/test/java/org/coffee/GreetingResourceIT.java
    ```

---

## Step 2 — Create the Order Model

Create `src/main/java/org/coffee/Order.java`:

```bash
mkdir -p src/main/java/org/coffee && touch src/main/java/org/coffee/Order.java
```

Open `Order.java` in your IDE and paste in the following:

```java
package org.coffee;

public class Order {
    public String itemName;
    public int quantity;
    public String customerId;

    public Order() {}

    public Order(String itemName, int quantity, String customerId) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.customerId = customerId;
    }
}
```

---

## Step 3 — Create the OrderResource (Producer)

Create `src/main/java/org/coffee/OrderResource.java`:

```bash
touch src/main/java/org/coffee/OrderResource.java
```

Open `OrderResource.java` in your IDE and paste in the following:

```java
package org.coffee;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    @Channel("coffee-orders") // (1)
    Emitter<String> emitter;  // (2)

    @Inject
    ObjectMapper objectMapper;

    @POST
    public Response placeOrder(Order order) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(order); // (3)
        emitter.send(json);                                   // (4)
        return Response.status(Response.Status.ACCEPTED).entity(order).build();
    }
}
```

1. `@Channel("coffee-orders")` binds this emitter to the `coffee-orders` Kafka topic (configured in `application.properties`).
2. `Emitter<String>` is the imperative API for sending messages — inject it like any CDI bean.
3. Serialise the `Order` POJO to a JSON string using Jackson.
4. `emitter.send(json)` publishes the message to the Kafka topic asynchronously.

---

## Step 4 — Configure the Outgoing Channel

Open `src/main/resources/application.properties` in `order-service` and add:

```properties title="order-service/application.properties"
# Run on port 8081 so it doesn't clash with menu-service on 8080
quarkus.http.port=8081

# Kafka outgoing channel — DevServices starts the broker automatically
mp.messaging.outgoing.coffee-orders.connector=smallrye-kafka
mp.messaging.outgoing.coffee-orders.value.serializer=org.apache.kafka.common.serialization.StringSerializer

quarkus.swagger-ui.always-include=true
```

!!! note "What just happened?"
    Notice what you did **not** configure:

    - No `bootstrap.servers` — no Kafka broker URL
    - No `docker-compose.yml`
    - No `docker run` command

    As soon as `quarkus dev` starts, Quarkus sees `messaging-kafka` on the classpath with no broker URL configured and automatically starts a **Redpanda** container (a Kafka-compatible broker) using DevServices.

---

## Step 5 — Start order-service

In the `order-service` terminal, run:

=== "Quarkus CLI"

    ```bash
    quarkus dev
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:dev
    ```

Watch the terminal carefully. You'll see a line like:

```
Dev Services for Kafka started. Other Quarkus applications in dev mode will find the broker automatically.
```

Quarkus pulled and started a Redpanda container — and it took under 3 seconds.

---

## Step 6 — Add the Consumer to menu-service

Go back to the **`menu-service`** project (still running in its own terminal).

Add the Kafka extension:

=== "Quarkus CLI"

    ```bash
    quarkus ext add messaging-kafka
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:add-extension -Dextensions="messaging-kafka"
    ```

Create `src/main/java/org/coffee/OrderConsumer.java`:

```bash
touch src/main/java/org/coffee/OrderConsumer.java
```

Open `OrderConsumer.java` in your IDE and paste in the following:

```java
package org.coffee;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderConsumer {

    private static final Logger LOG = Logger.getLogger(OrderConsumer.class);

    @Incoming("coffee-orders") // (1)
    public void onOrder(String orderJson) { // (2)
        LOG.infof("☕ New order received: %s", orderJson);
    }
}
```

1. `@Incoming("coffee-orders")` binds this method to the `coffee-orders` topic channel.
2. Each message received from the topic calls this method with the message payload as a `String`.

Add the consumer channel config to `menu-service/src/main/resources/application.properties`:

```properties title="menu-service/application.properties"
# Kafka incoming channel
mp.messaging.incoming.coffee-orders.connector=smallrye-kafka
mp.messaging.incoming.coffee-orders.value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
```

Save. `menu-service` live-reloads and automatically connects to the **same Redpanda container** that `order-service` started — no broker URL needed.

!!! note "What just happened?"
    Quarkus DevServices coordinates across dev mode processes. When `menu-service` starts and also has `messaging-kafka` with no broker URL, it discovers the already-running Redpanda container and connects to it automatically. Two services, one broker, zero config.

---

## Step 7 — Test the Full Flow

Open Swagger UI for `order-service` at `http://localhost:8081/q/swagger-ui`.

**POST an order:**

```json
{
  "itemName": "Espresso",
  "quantity": 2,
  "customerId": "alice"
}
```

Click **Execute** — you get HTTP 202 Accepted.

Now check the **`menu-service` terminal**:

```
INFO  [org.coffee.OrderConsumer] ☕ New order received: {"itemName":"Espresso","quantity":2,"customerId":"alice"}
```

The message traveled from `order-service` → Kafka → `menu-service` — **with no Kafka infrastructure setup at all**.

!!! note "Notice what you didn't do"
    - You didn't install Kafka
    - You didn't write a `docker-compose.yml`
    - You didn't set a broker URL anywhere
    - You didn't create a topic — DevServices created it automatically

    This is the DevServices promise: the infrastructure matches your code, automatically, in dev mode.

---

## Step 8 — Kafka Dev UI Panel

Open Dev UI for `order-service` at `http://localhost:8081/q/dev-ui`.

Find the **SmallRye Reactive Messaging** card. Click it to see:

- All configured channels (`coffee-orders`)
- Message count per channel
- An option to **send a test message** directly from the browser

Send a test message from the Dev UI and watch it appear in the `menu-service` terminal.

---

## Summary

| What | How |
|------|-----|
| ✅ Created a Kafka producer | `@Channel` + `Emitter<String>` |
| ✅ Created a Kafka consumer | `@Incoming("coffee-orders")` |
| ✅ Zero infrastructure setup | DevServices auto-started Redpanda |
| ✅ Two services share one broker | DevServices coordinates automatically |

!!! tip "Stuck or fell behind?"
    Complete solutions are in `labs/lab4-kafka/solution/`. **Run each command in a separate terminal** — each service needs its own shell:

    Terminal 1:
    ```bash
    cd labs/lab4-kafka/solution/order-service && quarkus dev
    ```

    Terminal 2:
    ```bash
    cd labs/lab4-kafka/solution/menu-service && quarkus dev
    ```

---

[← Lab 3: Config & Health](lab3-config-health.md){ .md-button }
[→ Lab 5: OIDC Security](lab5-security.md){ .md-button .md-button--primary }
