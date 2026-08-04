# Lab 1: Your First REST API

**Duration:** 12 minutes &nbsp;|&nbsp; **Project:** `menu-service`

!!! info "What you'll build"
    A `menu-service` REST API that lists and accepts coffee menu items, served with auto-generated Swagger UI docs.
    You'll experience Quarkus **live reload** and **continuous testing** — two features that make Quarkus development uniquely fast.

**Extensions used:**

| Extension | Purpose |
|-----------|---------|
| `quarkus-rest-jackson` | JAX-RS REST endpoints with JSON serialisation |
| `quarkus-smallrye-openapi` | Auto-generates OpenAPI spec + Swagger UI |

---

## Step 1 — Bootstrap the Project

Run the following command in your terminal:

=== "Quarkus CLI"

    ```bash
    quarkus create app org.coffee:menu-service \
      --extensions=rest-jackson,smallrye-openapi \
      --no-code
    cd menu-service
    ```

=== "Maven"

    ```bash
    mvn io.quarkus.platform:quarkus-maven-plugin:3.33.3:create \
      -DprojectGroupId=org.coffee \
      -DprojectArtifactId=menu-service \
      -Dextensions=rest-jackson,smallrye-openapi \
      -DnoCode
    cd menu-service
    ```

!!! note "What just happened?"
    Quarkus generated a complete Maven project in seconds with your chosen extensions already wired into `pom.xml`. No dependency hunting, no version alignment — the Quarkus BOM manages all of that.

Now open the project in your IDE. The structure looks like this:

```
menu-service/
├── src/
│   ├── main/
│   │   ├── java/org/coffee/           ← your application code goes here
│   │   └── resources/
│   │       └── application.properties ← all config in one place
│   └── test/
│       └── java/org/coffee/           ← your tests go here
└── pom.xml                            ← extensions declared here
```

Open `pom.xml` and notice your two extensions are already listed — no manual XML to write.

---

## Step 2 — Create the MenuItem Class

Create the file `src/main/java/org/coffee/MenuItem.java`:

```bash
mkdir -p src/main/java/org/coffee && touch src/main/java/org/coffee/MenuItem.java
```

```java
package org.coffee;

public class MenuItem {
    public String name;
    public String description;
    public double price;

    public MenuItem() {}

    public MenuItem(String name, String description, double price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }
}
```

!!! note "What just happened?"
    This is a plain Java object (POJO). Jackson — included via `quarkus-rest-jackson` — will automatically serialise it to and from JSON. No annotations needed on the class itself.

---

## Step 3 — Create the MenuResource

Create the file `src/main/java/org/coffee/MenuResource.java`:

```bash
touch src/main/java/org/coffee/MenuResource.java
```

```java
package org.coffee;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/menu")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MenuResource {

    // In-memory list for now — replaced with a real DB in Lab 2
    private static final List<MenuItem> items = new ArrayList<>(List.of(
        new MenuItem("Espresso", "A concentrated shot of coffee", 2.50),
        new MenuItem("Cappuccino", "Espresso with steamed milk foam", 3.75),
        new MenuItem("Cold Brew", "12-hour cold-steeped coffee", 4.00)
    ));

    @GET
    public List<MenuItem> list() {
        return items;
    }

    @POST
    public Response add(MenuItem item) {
        items.add(item);
        return Response.status(Response.Status.CREATED).entity(item).build();
    }
}
```

!!! note "What just happened?"
    - `@Path("/menu")` maps this class to the `/menu` URL path.
    - `@GET` on `list()` handles `GET /menu` — returns the list as a JSON array.
    - `@POST` on `add()` handles `POST /menu` — accepts a JSON body, adds it, returns HTTP 201 Created.
    - `@Produces` and `@Consumes` tell JAX-RS to use JSON for both input and output.

---

## Step 4 — Start Dev Mode

In your terminal (inside the `menu-service` directory), run:

=== "Quarkus CLI"

    ```bash
    quarkus dev
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:dev
    ```

You'll see output like this:

```
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
2024-xx-xx INFO  [io.quarkus] menu-service 1.0.0-SNAPSHOT on JVM started in 1.2s.
2024-xx-xx INFO  [io.quarkus] Listening on: http://localhost:8080
2024-xx-xx INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
```

!!! note "What just happened?"
    `quarkus dev` starts your application in **Dev Mode**. Notice:

    - It started in about **1 second** — fast for a JVM app.
    - **"Live Coding activated"** — this means Quarkus is watching your source files and will recompile on the next request whenever you save a change.
    - **Do not stop this terminal.** Keep Dev Mode running for the rest of this lab.

Open a browser and go to `http://localhost:8080/menu`. You should see:

```json
[
  {"name":"Espresso","description":"A concentrated shot of coffee","price":2.5},
  {"name":"Cappuccino","description":"Espresso with steamed milk foam","price":3.75},
  {"name":"Cold Brew","description":"12-hour cold-steeped coffee","price":4.0}
]
```

---

## Step 5 — Live Coding (No Restart Needed)

This is one of Quarkus' most distinctive features. Let's prove it works.

**While Dev Mode is still running**, open `MenuResource.java` in your IDE and change the first item's description — for example, change `"A concentrated shot of coffee"` to `"Bold & intense — the classic choice"`:

```java
private static final List<MenuItem> items = new ArrayList<>(List.of(
    new MenuItem("Espresso", "Bold & intense — the classic choice", 2.50), // (1)
    new MenuItem("Cappuccino", "Espresso with steamed milk foam", 3.75),
    new MenuItem("Cold Brew", "12-hour cold-steeped coffee", 4.00)
));
```

1. Change this description — don't touch anything else.

**Save the file.** Now refresh `http://localhost:8080/menu` in your browser.

You'll see the updated description — **without restarting the server**.

!!! tip "How does live reload work?"
    Quarkus doesn't poll your files. When you make the next HTTP request after saving, Quarkus detects that the source has changed, recompiles only the changed class (takes ~100ms), and serves the new version — all within the same request cycle. You never wait for a restart.

    This works for: Java classes, `application.properties`, templates, and static resources.

---

## Step 6 — Continuous Testing

Continuous testing means your tests re-run automatically every time you save a file — **without leaving your IDE or typing any command**.

First, create the test file at `src/test/java/org/coffee/MenuResourceTest.java`:

```bash
mkdir -p src/test/java/org/coffee && touch src/test/java/org/coffee/MenuResourceTest.java
```

```java
package org.coffee;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest // (1)
class MenuResourceTest {

    @Test
    void testGetMenuReturnsItems() {
        given()
            .when().get("/menu")
            .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1)); // (2)
    }

    @Test
    void testPostMenuAddsItem() {
        String newItem = """
            {"name":"Latte","description":"Smooth espresso with milk","price":4.25}
            """;
        given()
            .contentType("application/json")
            .body(newItem)
            .when().post("/menu")
            .then()
                .statusCode(201)
                .body("name", is("Latte")); // (3)
    }
}
```

1. `@QuarkusTest` starts your Quarkus app in test mode. No separate test server needed.
2. Assert the response array has at least one item.
3. Assert the returned JSON has `"name": "Latte"`.

Now activate continuous testing. **In the same terminal where `quarkus dev` is running**, press:

```
r
```

You'll see the tests run immediately:

```
All 2 tests are passing (2 passing, 0 failing)
```

!!! note "What just happened?"
    You pressed `r` once and continuous testing is now active. From this point on, **every time you save any file**, Quarkus re-runs the affected tests automatically. You'll see results in the terminal in real time — no manual `mvn test` needed.

    Try it: make a deliberate mistake in `MenuResource.java` (e.g., change `201` to `200` in the POST response), save it, and watch the test turn red. Fix it, save, watch it turn green.

!!! tip "Other Dev Mode keyboard shortcuts"
    While `quarkus dev` is running, you can press:

    - `r` — toggle continuous testing on/off
    - `o` — open the Dev UI in your browser
    - `s` — force restart the application
    - `h` — show all available commands

---

## Step 7 — Swagger UI Tour

`quarkus-smallrye-openapi` automatically generates an OpenAPI specification from your resource annotations and serves a Swagger UI — with **zero configuration**.

Open your browser and navigate to:

```
http://localhost:8080/q/swagger-ui
```

You'll see a fully interactive API documentation page with your two endpoints listed.

**Try `GET /menu`:**

1. Click on `GET /menu`
2. Click **Try it out**
3. Click **Execute**
4. See the JSON response with your 3 coffee items appear below

**Try `POST /menu`:**

1. Click on `POST /menu`
2. Click **Try it out**
3. Replace the request body with:
    ```json
    {
      "name": "Oat Milk Latte",
      "description": "Espresso with creamy oat milk",
      "price": 4.75
    }
    ```
4. Click **Execute**
5. See HTTP 201 and the new item echoed back

Now go to `GET /menu` again and execute it — your new item is in the list.

!!! note "What just happened?"
    Quarkus reads your `@Path`, `@GET`, `@POST`, `@Produces`, and `@Consumes` annotations at build time and generates an OpenAPI 3.0 spec automatically. The Swagger UI is served at `/q/swagger-ui` in Dev Mode (and can be optionally enabled in production too).

    The raw OpenAPI spec is also available at `http://localhost:8080/q/openapi`.

---

## Summary

In 12 minutes you have:

| What | How |
|------|-----|
| ✅ Bootstrapped a Quarkus project | `quarkus create app` with two extensions |
| ✅ Built a working JSON REST API | `@Path`, `@GET`, `@POST` on a resource class |
| ✅ Used live reload | Edited code and saw changes without restarting |
| ✅ Ran continuous tests | Pressed `r` — tests run on every save |
| ✅ Explored Swagger UI | Interactive API docs with zero configuration |

!!! tip "Stuck or fell behind?"
    The complete solution for this lab is in `labs/lab1-rest/solution/`. Run it with:

    === "Quarkus CLI"
        ```bash
        cd labs/lab1-rest/solution
        quarkus dev
        ```
    === "Maven"
        ```bash
        cd labs/lab1-rest/solution
        ./mvnw quarkus:dev
        ```

---

[← Prerequisites](00-prerequisites.md){ .md-button }
[→ Lab 2: Panache ORM](lab2-panache.md){ .md-button .md-button--primary }
