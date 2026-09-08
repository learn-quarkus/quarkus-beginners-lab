# Lab 10: Quarkus Flow — Agentic Order Workflow

**Duration:** 12 minutes &nbsp;|&nbsp; **Projects:** `order-flow-service` (new) + `barista-bot` (from Lab 8)

!!! warning "Labs 4 and 8 required"
    This lab builds on `order-service` from Lab 4 and `barista-bot` from Lab 8.
    If you didn't complete Lab 8, run the setup script below to get the starting point.

!!! info "What you'll build"
    - **`order-flow-service`** — a new Quarkus app on port `8082` that runs a **Quarkus Flow** workflow: it calls `order-service` to record an order, then checks whether the total exceeds a configurable threshold.
    - **`barista-bot`** — updated with a `@Tool`-annotated `placeOrder` method so the LLM can place real orders through the chat window.

    **Result:** Type *"I'd like 4 lattes please"* in the barista chat. The LLM looks up the price via MCP, calls the `placeOrder` tool, and the workflow either confirms the order immediately or pauses it for barista approval — all from a single chat message.

!!! note "Quarkus Flow in one sentence"
    **Quarkus Flow** is a lightweight workflow engine built into Quarkus, based on the CNCF Open Workflow spec. You model workflows as Java code, and Quarkus handles execution, observability, and the Dev UI visualisation for free.

**Reference:** [Quarkus Flow docs](https://docs.quarkiverse.io/quarkus-flow/dev/index.html){ target="_blank" }

---

!!! tip "Working directory"
    Unless otherwise noted, commands for `order-flow-service` run from the `order-flow-service/` directory you create in Step 1.
    Commands for `barista-bot` run from your existing `barista-bot/` directory.

---

## Setup — Get the starting point for `barista-bot`

If you completed Lab 8 your `barista-bot` directory is already ready. Skip to [Step 1](#step-1-create-order-flow-service).

If you didn't finish Lab 8, run the setup script from the repo root:

```bash
bash labs/lab10-quarkus-flow/setup.sh
```

The script does six things automatically:

1. Copies the Lab 8 `barista-bot` solution into `workshop/barista-bot/`
2. Applies the Lab 10 chat template with order-focused prompt chips
3. Adds the `quarkus-rest-client-jackson` extension to `pom.xml`
4. Writes `OrderRequest.java`, `OrderResult.java`, `OrderFlowClient.java`, and `OrderTools.java`
5. Replaces `BaristaAiService.java` with the `@ToolBox`-enabled version
6. Appends the `order-flow-service` REST client URL and updated MCP port to `application.properties`

It is **idempotent** — safe to run again if something goes wrong.

---

## Step 1 — Create `order-flow-service`

In a **new terminal**, create the project:

=== "Quarkus CLI"

    ```bash
    quarkus create app org.coffee:order-flow-service \
      --extensions=rest-jackson,rest-client-jackson
    cd order-flow-service
    ```

=== "Maven"

    ```bash
    mvn io.quarkus.platform:quarkus-maven-plugin:3.33.3:create \
      -DprojectGroupId=org.coffee \
      -DprojectArtifactId=order-flow-service \
      -Dextensions=rest-jackson,rest-client-jackson
    cd order-flow-service
    ```

!!! tip "Delete the generated sample files"
    ```bash
    rm src/main/java/org/coffee/GreetingResource.java
    rm src/test/java/org/coffee/GreetingResourceTest.java
    rm src/test/java/org/coffee/GreetingResourceIT.java
    ```

---

## Step 2 — Add the Quarkus Flow extension

Quarkus Flow is a Quarkiverse extension — add it explicitly:

=== "Quarkus CLI"

    ```bash
    quarkus extension add io.quarkiverse.flow:quarkus-flow
    ```

=== "Maven"

    Add to `pom.xml` inside `<dependencyManagement>` (alongside the Quarkus BOM):

    ```xml title="pom.xml — dependencyManagement"
    <dependency>
      <groupId>io.quarkiverse.flow</groupId>
      <artifactId>quarkus-flow-bom</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    ```

    Then add to `<dependencies>`:

    ```xml title="pom.xml — dependencies"
    <dependency>
      <groupId>io.quarkiverse.flow</groupId>
      <artifactId>quarkus-flow</artifactId>
    </dependency>
    ```

!!! note "What just happened?"
    - **`quarkus-flow`** — the workflow engine: Java DSL, Dev UI, execution tracing.

    This workflow is pure Java — it calls `order-service` and branches on a config
    value, with no LLM inside the flow. So you only need `quarkus-flow` itself; the
    `quarkus-flow-langchain4j` bridge and an LLM provider are **not** required here.
    (The AI lives in `barista-bot`, which calls this service over REST.)

---

## Step 3 — Configure `application.properties`

Open `src/main/resources/application.properties` and replace its contents with:

```properties title="application.properties"
# Port — barista-bot runs on 8080, order-flow-service on 8082
quarkus.http.port=8082

# REST client pointing at order-service (Lab 4)
quarkus.rest-client.order-service.url=http://localhost:8081

# High-value order threshold — orders at or above this amount require barista approval.
# Change this value and save; Quarkus live-reload picks it up instantly.
coffee.approval.threshold=15.0
```

!!! note "What just happened?"
    `coffee.approval.threshold` is a plain MicroProfile Config property injected with `@ConfigProperty`. Try setting it to `0.0` to force every order through the approval path, or `999.0` to confirm all orders immediately — with Quarkus dev mode running, just save the file and the change takes effect without a restart.

---

## Step 4 — Add the `Order` model and `OrderServiceClient`

Create `src/main/java/org/coffee/Order.java`:

```java title="Order.java"
package org.coffee;

public class Order {

    public String itemName;
    public int    quantity;
    public String customerId;
    public double totalPrice; // (1)

    public Order() {}

    public Order(String itemName, int quantity, String customerId, double totalPrice) {
        this.itemName   = itemName;
        this.quantity   = quantity;
        this.customerId = customerId;
        this.totalPrice = totalPrice;
    }
}
```

1. `totalPrice` extends the Lab 4 `Order` POJO with the price needed for the threshold check. `order-service` ignores unknown fields, so nothing breaks on that end.

Create `src/main/java/org/coffee/OrderServiceClient.java`:

```java title="OrderServiceClient.java"
package org.coffee;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "order-service") // (1)
@Path("/orders")
public interface OrderServiceClient {

    @POST
    Response placeOrder(Order order);
}
```

1. `configKey = "order-service"` links this interface to the `quarkus.rest-client.order-service.url` property you set in Step 3. No URL hard-coding needed.

!!! note "What just happened?"
    `@RegisterRestClient` is the same type-safe HTTP client pattern introduced in Lab 8 for the MCP client. Declare the interface; Quarkus generates the implementation at build time.

Finally, create `src/main/java/org/coffee/OrderState.java` — the object that flows
between the workflow's tasks:

```java title="OrderState.java"
package org.coffee;

public class OrderState {

    public String orderId;
    public String itemName;
    public int    quantity;
    public String customerId;
    public double totalPrice;
    public String status; // (1) "CONFIRMED" or "PENDING_APPROVAL"

    public OrderState() {}
}
```

1. Each workflow task takes an input type and returns an output type. Using a typed
   POJO (rather than a raw `Map`) lets Quarkus Flow deserialise the workflow data for
   you: `placeOrder` produces an `OrderState`, the switch inspects its `totalPrice`,
   and a branch task stamps the final `status`.

---

## Step 5 — Define the `OrderFlowWorkflow`

This is the heart of the lab. Create `src/main/java/org/coffee/OrderFlowWorkflow.java`:

```java title="OrderFlowWorkflow.java"
package org.coffee;

import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Workflow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.UUID;

import static io.quarkiverse.flow.dsl.FlowDSL.function;
import static io.quarkiverse.flow.dsl.FlowDSL.switchWhenOrElse;
import static io.quarkiverse.flow.dsl.FlowWorkflowBuilder.workflow;

@ApplicationScoped
public class OrderFlowWorkflow extends Flow { // (1)

    @RestClient
    OrderServiceClient orderServiceClient;

    @ConfigProperty(name = "coffee.approval.threshold", defaultValue = "15.0")
    double threshold; // (2)

    @Override
    public Workflow descriptor() {
        // Capture the config value so the switch predicate closes over the
        // primitive, not the CDI bean.
        final double approvalThreshold = threshold;

        return workflow("order-flow")
            .tasks(
                // Step 1: generate an orderId and call order-service
                function("placeOrder", (Order order) -> { // (3)
                    String orderId = UUID.randomUUID().toString().substring(0, 8);

                    try (Response resp = orderServiceClient.placeOrder(order)) {
                        // order-service returns 202; orderId is generated here
                    }

                    OrderState state = new OrderState();
                    state.orderId    = orderId;
                    state.itemName   = order.itemName;
                    state.quantity   = order.quantity;
                    state.customerId = order.customerId;
                    state.totalPrice = order.totalPrice;
                    return state;
                }, Order.class),

                // Step 2: branch on the threshold
                switchWhenOrElse( // (4)
                    (OrderState state) -> state.totalPrice >= approvalThreshold,
                    "requireApproval",   // high-value path
                    "confirmOrder",      // low-value path
                    OrderState.class),

                // High-value — barista must approve. End here so we don't fall
                // through into confirmOrder.
                function("requireApproval", (OrderState state) -> {
                    state.status = "PENDING_APPROVAL";
                    return state;
                }, OrderState.class).then(FlowDirectiveEnum.END), // (5)

                // Low-value — confirm straight away
                function("confirmOrder", (OrderState state) -> {
                    state.status = "CONFIRMED";
                    return state;
                }, OrderState.class)
            )
            .build();
    }
}
```

1. Extend `Flow` — Quarkus discovers every `Flow` subclass at build time and registers it in the workflow registry. You never call `new OrderFlowWorkflow()`.
2. `@ConfigProperty` reads `coffee.approval.threshold` from `application.properties`. In dev mode, changing the value and saving hot-reloads the workflow automatically.
3. `function(name, fn, InputType.class)` runs plain Java as a workflow task. The input is deserialised to `Order`; whatever the lambda returns (here an `OrderState`) becomes the workflow data passed to the next task.
4. `switchWhenOrElse(predicate, thenTask, elseTask, Type.class)` routes to a **named** task: high-value orders jump to `requireApproval`, everything else to `confirmOrder`.
5. `.then(FlowDirectiveEnum.END)` stops the workflow after `requireApproval`. Without it, execution would fall through into the next task in the list (`confirmOrder`) and overwrite the status.

!!! note "What just happened?"
    You just wrote a two-step, conditionally branching workflow using the Quarkus Flow Java DSL. No XML, no YAML, no external orchestration engine. Open the Dev UI after starting the service to see an auto-generated Mermaid diagram of this exact topology.

---

## Step 6 — Expose the REST endpoints and barista admin UI

This step creates four files: a shared store, the main REST resource, the admin UI resource, and the admin HTML template.

### 6a — `PendingOrdersStore`

Both the REST resource and the admin UI need access to the same set of pending orders. Create a shared CDI bean so neither class holds its own private map.

Create `src/main/java/org/coffee/PendingOrdersStore.java`:

```java title="PendingOrdersStore.java"
package org.coffee;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class PendingOrdersStore {

    private final ConcurrentHashMap<String, OrderState> pending  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OrderState> completed = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<OrderState>      approved = new CopyOnWriteArrayList<>();

    public void add(OrderState order) {
        pending.put(order.orderId, order);
    }

    public OrderState get(String orderId) {
        return pending.get(orderId);
    }

    public OrderState approve(String orderId) { // (1)
        OrderState order = pending.remove(orderId);
        if (order != null) {
            order.status = "CONFIRMED";
            completed.put(orderId, order);
            approved.add(0, order);
        }
        return order;
    }

    public String status(String orderId) {
        OrderState waiting = pending.get(orderId);
        if (waiting != null) return waiting.status;
        OrderState finished = completed.get(orderId);
        return finished == null ? "CONFIRMED" : finished.status;
    }

    public Collection<OrderState> allPending() { return Collections.unmodifiableCollection(pending.values()); }
    public List<OrderState>       allApproved() { return Collections.unmodifiableList(approved); }
    public boolean                isEmpty()     { return pending.isEmpty(); }
}
```

1. `approve()` removes the order from `pending` and prepends it to `approved` (newest-first) so the admin UI can show a session history.

### 6b — `OrderFlowResource`

Create `src/main/java/org/coffee/OrderFlowResource.java`:

```java title="OrderFlowResource.java"
package org.coffee;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/flow")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderFlowResource {

    @Inject OrderFlowWorkflow  orderFlow;
    @Inject PendingOrdersStore store;

    @POST
    @Path("/order")
    @Blocking // (1)
    public Uni<Response> placeOrder(Order order) {
        return orderFlow
            .startInstance(order)
            .onItem().transform(model -> {
                OrderState result = model.as(OrderState.class).orElseThrow();
                if ("PENDING_APPROVAL".equals(result.status)) {
                    store.add(result);
                    return Response.accepted(result).build(); // 202
                }
                return Response.ok(result).build();           // 200
            });
    }

    @GET
    @Path("/status/{orderId}")
    public Response getOrderStatus(@PathParam("orderId") String orderId) { // (2)
        return Response.ok(Map.of("orderId", orderId, "status", store.status(orderId))).build();
    }

    @POST
    @Path("/approve/{orderId}")
    public Response approveOrder(@PathParam("orderId") String orderId) {
        OrderState pending = store.approve(orderId);
        if (pending == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Order " + orderId + " not found or already processed"))
                .build();
        }
        return Response.ok(pending).build();
    }
}
```

1. `@Blocking` is required because the REST client call inside the workflow is synchronous. Without it, Quarkus throws `BlockingNotAllowedException` on the Vert.x IO thread.
2. `GET /flow/status/{orderId}` lets `barista-bot` check whether a pending order has been approved without polling the admin page. An order absent from the pending store was either never held there or has already been approved.

### 6c — `AdminResource` and admin template

Add the `quarkus-rest-qute` extension so this service can serve HTML:

=== "Quarkus CLI"

    ```bash
    quarkus ext add rest-qute
    ```

=== "Maven"

    ```xml title="pom.xml — dependencies"
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-rest-qute</artifactId>
    </dependency>
    ```

Create `src/main/java/org/coffee/AdminResource.java`:

```java title="AdminResource.java"
package org.coffee;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/admin")
public class AdminResource {

    @Inject Template            admin;
    @Inject PendingOrdersStore  store;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(
        @QueryParam("approved") String approved,
        @QueryParam("error")    String error) {
      return admin
          .data("orders",         store.allPending())
          .data("approvedOrders", store.allApproved())
          .data("approved",       approved)
          .data("error",          error);
    }

    @POST
    @Path("/approve/{orderId}")
    public Response approve(@PathParam("orderId") String orderId) {
        OrderState order = store.approve(orderId);
        if (order == null) {
            return Response.seeOther(URI.create("/admin?error=" + orderId)).build();
        }
        return Response.seeOther(URI.create("/admin?approved=" + orderId)).build();
    }
}
```

Create `src/main/resources/templates/admin.html` — copy it from the solution:

```
labs/lab10-quarkus-flow/solution/order-flow-service/src/main/resources/templates/admin.html
```

!!! note "What just happened?"
    The barista admin page at **`http://localhost:8082/admin`** auto-refreshes every 5 seconds. Pending orders appear as cards with an **Approve** button; approved orders move to a history section below. The `PendingOrdersStore` bean is the single source of truth shared by both `OrderFlowResource` and `AdminResource`.

---

## Step 7 — Update `barista-bot` to place orders

Switch to your `barista-bot` directory.

### 7a — Add the REST client extension

=== "Quarkus CLI"

    ```bash
    quarkus ext add rest-client-jackson
    ```

=== "Maven"

    ```bash
    mvn quarkus:add-extension -Dextensions="rest-client-jackson"
    ```

### 7b — Add three new files

Create `src/main/java/org/coffee/OrderRequest.java`:

```java title="OrderRequest.java"
package org.coffee;

public class OrderRequest {
    public String itemName;
    public int    quantity;
    public String customerId;
    public double totalPrice;

    public OrderRequest() {}

    public OrderRequest(String itemName, int quantity, String customerId, double totalPrice) {
        this.itemName   = itemName;
        this.quantity   = quantity;
        this.customerId = customerId;
        this.totalPrice = totalPrice;
    }
}
```

Create `src/main/java/org/coffee/OrderResult.java`:

```java title="OrderResult.java"
package org.coffee;

public class OrderResult {
    public String orderId;
    public String status; // "CONFIRMED" or "PENDING_APPROVAL"

    public OrderResult() {}
}
```

Create `src/main/java/org/coffee/OrderFlowClient.java`:

```java title="OrderFlowClient.java"
package org.coffee;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "order-flow-service")
@Path("/flow")
public interface OrderFlowClient {

    @POST
    @Path("/order")
    OrderResult placeOrder(OrderRequest request);

    @GET
    @Path("/status/{orderId}")
    OrderResult getOrderStatus(@PathParam("orderId") String orderId); // (1)
}
```

1. Calls `GET /flow/status/{orderId}` on `order-flow-service`. Returns `PENDING_APPROVAL` if the order is still waiting, `CONFIRMED` once the barista approves.

Create `src/main/java/org/coffee/OrderTools.java`:

```java title="OrderTools.java"
package org.coffee;

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class OrderTools {

    @RestClient
    @Inject
    OrderFlowClient orderFlowClient;

    @Tool("Place a coffee order. Returns the order ID and status. " // (1)
        + "Use getItemPrice first to calculate totalPrice = price × quantity.")
    public String placeOrder(
            String item_name,     // (2)
            int    quantity,
            String customer_id,
            double total_price) {

        OrderResult result = orderFlowClient.placeOrder(
            new OrderRequest(item_name, quantity, customer_id, total_price));

        if ("PENDING_APPROVAL".equals(result.status)) {
            return "Order received but requires barista approval (total $"
                   + String.format("%.2f", total_price) + " exceeds the express limit). "
                   + "Order ID: " + result.orderId + ". "
                   + "Tell the customer their order is pending and they can ask you to "
                   + "check the status at any time using the order ID.";
        }
        return "Order confirmed! ☕ " + quantity + "× " + item_name
               + " — Order #" + result.orderId
               + ". Total: $" + String.format("%.2f", total_price);
    }

    @Tool("Check the current status of an order by its order ID. " // (3)
        + "Call this when the customer asks about their order status or whether it has been approved.")
    public String getOrderStatus(String order_id) {
        OrderResult result = orderFlowClient.getOrderStatus(order_id);
        if ("PENDING_APPROVAL".equals(result.status)) {
            return "Order #" + order_id + " is still awaiting barista approval.";
        }
        return "Order #" + order_id + " has been confirmed! ☕ Your order is on its way.";
    }
}
```

1. The `@Tool` description is sent to the LLM as part of the tool schema. Write it like a docstring — the model reads it to decide when to call this method.
2. Tool method parameters must use `snake_case` for compatibility with LLM tool-call schemas.
3. The second `@Tool` gives the LLM a live status check — it calls `GET /flow/status/{orderId}` in real time rather than relying on what the conversation history says.

### 7c — Update `BaristaAiService`

Open `BaristaAiService.java` and update it to include the new `@ToolBox`:

```java title="BaristaAiService.java"
package org.coffee;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.quarkiverse.langchain4j.ToolBox;
import io.quarkiverse.langchain4j.mcp.runtime.McpToolBox;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
@SystemMessage("""
    You are a friendly and knowledgeable barista at The Quarkus Cafe.
    Answer questions about coffee, our menu, and brewing methods.
    Keep responses concise — 2-3 sentences maximum.
    If asked about something unrelated to coffee, politely redirect the conversation.
    When answering menu questions, use the available tools to get accurate, up-to-date information.
    When a customer asks to order, place, or buy an item:
      1. Use getItemPrice to look up the price per item.
      2. Calculate totalPrice = price × quantity.
      3. Use the placeOrder tool to submit the order.
    When a customer asks about the status of an order or whether it has been approved:
      1. Extract the order ID from the conversation history.
      2. Call the getOrderStatus tool with that order ID.
      3. Report the result clearly — confirmed or still pending.
    Never guess or assume an order status — always call getOrderStatus to check.
    """)
public interface BaristaAiService {

    @McpToolBox("menu")
    @ToolBox(OrderTools.class) // (1)
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
```

1. `@ToolBox(OrderTools.class)` wires the CDI bean's `@Tool` methods into this AI service. The LLM now has the MCP menu tools, `placeOrder`, and `getOrderStatus` available on every request.

### 7d — Update `application.properties`

Add the REST client config for `order-flow-service`:

```properties title="application.properties (append)"
# REST client — connect to order-flow-service on port 8082
quarkus.rest-client.order-flow-service.url=http://localhost:8082
```

In this lab `menu-mcp-server` runs on **8084** (Step 8), so update the MCP menu URL from Lab 8:

```properties title="application.properties (update the Lab 8 value)"
quarkus.langchain4j.mcp.menu.url=http://localhost:8084/mcp/sse
```

!!! note "What just happened?"
    The LLM now has a three-tool chain: `getItemPrice` (MCP) for prices, `placeOrder` to submit the order, and `getOrderStatus` to check it live. The system message instructs the model exactly when to call each one.

### 7e — Add the End Chat endpoint to `ChatUiResource`

Open `src/main/java/org/coffee/ChatUiResource.java` and add an `/end` endpoint that clears both the display history and the LangChain4j memory store:

```java title="ChatUiResource.java — add imports"
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.net.URI;
```

```java title="ChatUiResource.java — add field"
@Inject
ChatMemoryStore chatMemoryStore;
```

```java title="ChatUiResource.java — add endpoint"
@POST
@Path("/end")
public Response end(@CookieParam("session") String session) {
    if (session != null && !session.isBlank()) {
        sessions.remove(session);
        chatMemoryStore.deleteMessages(session); // (1)
    }
    NewCookie expired = new NewCookie.Builder("session")
            .value("").path("/").maxAge(0).build();
    return Response.seeOther(URI.create("/"))
            .cookie(expired)
            .build();
}
```

1. `deleteMessages` evicts the LangChain4j in-memory conversation history for this session ID. The next chat starts completely fresh — no memory of previous orders or messages.

The **End Chat** button in `chat.html` already targets this endpoint via a hidden form. If you copied the template from the solution in Step 7b, no template changes are needed.

---

## Step 8 — Run and test everything

### Starting all services

Instead of managing four terminals manually, use the provided script from the repo root:

```bash
export QUARKUS_LANGCHAIN4J_OPENAI_API_KEY=sk-...
bash labs/lab10-quarkus-flow/start-services.sh
```

The script starts all four services in dependency order, waits for each port to be live, and tails all logs to one terminal. Press **Ctrl-C** to stop everything cleanly.

| Service | Port | URL |
|---|---|---|
| `barista-bot` | 8080 | http://localhost:8080 |
| `order-service` | 8081 | — |
| `order-flow-service` | 8082 | http://localhost:8082/admin |
| `menu-mcp-server` | 8084 | — |

Open **`http://localhost:8080`** for the chat and **`http://localhost:8082/admin`** for the barista approval page in separate browser tabs.

---

### Demo 1 — Low-value order (below threshold)

Type in the chat:

> *"Can I get 2 espressos please? My name is Alex."*

Watch the `order-flow-service` log. You'll see the workflow execute both tasks and return `CONFIRMED`. The bot replies:

> *"Your order is confirmed! ☕ 2× Espresso — Order #a3f1b2c4. Total: $5.00"*

---

### Demo 2 — High-value order + admin approval

Type in the chat:

> *"I'd like 5 lattes for the team — customer name is Dev Team."*

5 × $4.25 = **$21.25** — above the $15.00 threshold. The bot replies:

> *"Your order has been received but requires barista approval (total $21.25 exceeds the express limit). Order ID: d9e2f3a1. A barista will confirm it shortly."*

Switch to the **admin tab** at **`http://localhost:8082/admin`**. The order card appears automatically (the page auto-refreshes every 5 seconds). Click **Approve** — the card moves to the "Approved this session" history with a green badge.

Back in the chat, ask:

> *"Has my order been approved?"*

The bot calls `getOrderStatus` live, gets `CONFIRMED`, and replies:

> *"Order #d9e2f3a1 has been confirmed! ☕ Your order is on its way."*

---

### Demo 3 — End Chat

Click **End Chat** in the chat UI. This calls `POST /end`, which:

- Removes the display history for this session
- Evicts the LangChain4j memory (`chatMemoryStore.deleteMessages`)
- Expires the session cookie

The next message starts a completely fresh conversation with no memory of previous orders.

---

!!! tip "Try it: change the threshold"
    While `order-flow-service` is running, open `order-flow-service/src/main/resources/application.properties` and change:

    ```properties
    coffee.approval.threshold=5.0
    ```

    Save the file. Quarkus detects the change and live-reloads the workflow — no restart needed. Now order a single espresso ($2.50) and watch it go through the approval path. Change it back to `15.0` to restore normal behaviour.

---

!!! tip "Open the Flow Dev UI"
    Navigate to **`http://localhost:8082/q/dev-ui`** and look for the **Quarkus Flow** panel.

    You'll see:

    - An auto-generated **Mermaid diagram** of the `order-flow` workflow topology — two tasks, one conditional branch
    - **Execution history** for every `startInstance` call, with timestamps and the final output JSON
    - A **trigger form** so you can fire test workflows directly from the browser

    This is the same observability you get for free with any `Flow` subclass — no configuration required.

---

## Summary

| What | How |
|------|-----|
| ✅ Workflow with conditional branch | `switchWhenOrElse(predicate, thenTask, elseTask, Type)` in the Flow Java DSL |
| ✅ Configurable approval threshold | `@ConfigProperty` + `coffee.approval.threshold` in `application.properties` |
| ✅ Type-safe call to `order-service` | `@RegisterRestClient` interface — same pattern as Lab 8's MCP client |
| ✅ LLM places orders via chat | `@Tool` on a CDI bean + `@ToolBox` on the AI service method |
| ✅ LLM checks order status live | `getOrderStatus` `@Tool` calls `GET /flow/status/{orderId}` — never guesses |
| ✅ Barista admin UI | `AdminResource` + Qute template at `http://localhost:8082/admin` — auto-refreshes, shows history |
| ✅ Clean session end | `POST /end` clears display history + `ChatMemoryStore` + expires cookie |
| ✅ One-command startup | `start-services.sh` starts all four services in order, unified log tail, Ctrl-C stops all |
| ✅ Live-reload config changes | Quarkus dev mode hot-reloads `@ConfigProperty` values on file save |
| ✅ Free observability | Flow Dev UI: Mermaid diagram, execution traces, trigger form |

!!! tip "Stuck or fell behind?"
    Complete solutions are in `labs/lab10-quarkus-flow/solution/`:

    ```bash
    # Terminal 1 — order-service (Lab 4):
    cd labs/lab4-kafka/solution/order-service && quarkus dev

    # Terminal 2 — menu-mcp-server on port 8084 (avoids clash with order-service):
    cd labs/lab8-mcp-server/menu-mcp-server && quarkus dev -Dquarkus.http.port=8084

    # Terminal 3 — order-flow-service:
    cd labs/lab10-quarkus-flow/solution/order-flow-service && quarkus dev

    # Terminal 4 — barista-bot with placeOrder tool:
    cd labs/lab10-quarkus-flow/solution/barista-bot && quarkus dev
    ```

---

[← Lab 9: Containerize](lab9-containerize.md){ .md-button }
[→ Wrap-Up](wrap-up.md){ .md-button .md-button--primary }
