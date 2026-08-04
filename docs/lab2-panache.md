# Lab 2: Panache ORM + H2 Database

**Duration:** 8 minutes &nbsp;|&nbsp; **Project:** `menu-service` (continued from Lab 1)

!!! info "What you'll build"
    Promote `MenuItem` from an in-memory POJO to a real JPA entity backed by an H2 in-memory database. Quarkus Panache eliminates DAO boilerplate — `listAll()`, `persist()`, `findById()` and more come for free on every entity class.

**New extensions added in this lab:**

| Extension | Purpose |
|-----------|---------|
| `quarkus-hibernate-orm-panache` | JPA with zero-boilerplate Active Record pattern |
| `quarkus-jdbc-h2` | In-memory H2 database — no installation needed |

---

## Step 1 — Add the Extensions

Make sure Dev Mode is **still running** from Lab 1 (`quarkus dev` or `./mvnw quarkus:dev`). Open a **second terminal** in the `menu-service` directory and run:

=== "Quarkus CLI"

    ```bash
    quarkus ext add hibernate-orm-panache,jdbc-h2
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:add-extension -Dextensions="hibernate-orm-panache,jdbc-h2"
    ```

Quarkus detects the `pom.xml` change and live-reloads automatically. You'll see the two new dependencies appear in `pom.xml`:

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-hibernate-orm-panache</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-jdbc-h2</artifactId>
</dependency>
```

!!! note "What just happened?"
    You added two extensions without touching `pom.xml` by hand. The Quarkus CLI writes the correct dependency — including the right version from the BOM — for you.

---

## Step 2 — Configure the Datasource

Open `src/main/resources/application.properties` and add:

```properties title="application.properties"
# H2 in-memory datasource
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:coffeedb;DB_CLOSE_DELAY=-1

# Auto-create schema on startup and drop it on shutdown
quarkus.hibernate-orm.database.generation=drop-and-create

# Log generated SQL so you can see what Hibernate is doing
quarkus.hibernate-orm.log.sql=true
```

!!! note "What just happened?"
    Three lines is all it takes to configure a fully working JPA datasource. The Quarkus BOM manages Hibernate and H2 versions — you never set them manually.

    - `db-kind=h2` — Quarkus picks the correct JDBC driver automatically.
    - `drop-and-create` — Hibernate creates tables on startup and drops them on shutdown. Perfect for dev.
    - `log.sql=true` — every SQL statement Hibernate executes will appear in the terminal. Great for learning what's happening under the hood.

!!! tip "What about production?"
    H2 is dev and test only — data is lost on restart. In production you'd use PostgreSQL. Just change `db-kind=postgresql` and set `jdbc.url` to your real database. Quarkus DevServices can also auto-start a real PostgreSQL container in dev with zero config — the same pattern you'll see with Kafka in Lab 4.

---

## Step 3 — Promote MenuItem to a JPA Entity

Replace the entire content of `src/main/java/org/coffee/MenuItem.java` with:

```java
package org.coffee;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity // (1)
public class MenuItem extends PanacheEntity { // (2)

    @Column(nullable = false)
    public String name;

    public String description;

    @Column(nullable = false)
    public double price;

    // JPA requires a no-arg constructor
    public MenuItem() {}

    public MenuItem(String name, String description, double price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }
}
```

1. `@Entity` tells JPA to map this class to a database table. Hibernate creates the table automatically.
2. `extends PanacheEntity` gives you `id` (auto-generated), `persist()`, `listAll()`, `findById()`, `count()`, `delete()` and many more — all for free.

!!! note "What just happened?"
    By extending `PanacheEntity`, your entity class gains a full suite of database operations as **static methods**. You don't write a DAO, a Repository interface, or an `EntityManager` injection. Panache does all of that behind the scenes.

    `PanacheEntity` automatically creates a `Long id` field. If you need a different id type or name, use `PanacheEntityBase` instead and declare your own.

---

## Step 4 — Update MenuResource to Use the Database

Replace `src/main/java/org/coffee/MenuResource.java` with:

```java
package org.coffee;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/menu")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MenuResource {

    @GET
    public List<MenuItem> list() {
        return MenuItem.listAll(); // (1)
    }

    @POST
    @Transactional // (2)
    public Response add(MenuItem item) {
        item.persist(); // (3)
        return Response.status(Response.Status.CREATED).entity(item).build();
    }
}
```

1. `MenuItem.listAll()` — Panache generates `SELECT * FROM MenuItem`. One line replaces a full DAO.
2. `@Transactional` — **required** for any write operation. Without it you'll get a `TransactionRequiredException` at runtime.
3. `item.persist()` — Panache generates `INSERT INTO MenuItem ...` and populates `item.id` with the new auto-generated value.

!!! warning "Don't forget @Transactional"
    Every method that writes to the database — `persist()`, `delete()`, `update()` — must run inside a transaction. Annotate the resource method (or a service layer method) with `@Transactional`. Forgetting it is the most common mistake in this lab.

---

## Step 5 — Seed Initial Data

Create the file `src/main/resources/import.sql`:

```bash
touch src/main/resources/import.sql
```

Open `import.sql` in your IDE and paste in the following:

```sql title="import.sql"
INSERT INTO MenuItem(id, name, description, price) VALUES (nextval('MenuItem_SEQ'), 'Espresso', 'A concentrated shot of coffee', 2.50);
INSERT INTO MenuItem(id, name, description, price) VALUES (nextval('MenuItem_SEQ'), 'Cappuccino', 'Espresso with steamed milk foam', 3.75);
INSERT INTO MenuItem(id, name, description, price) VALUES (nextval('MenuItem_SEQ'), 'Cold Brew', '12-hour cold-steeped coffee', 4.00);
```

!!! note "What just happened?"
    Quarkus automatically runs `import.sql` after Hibernate creates the schema when `database.generation=drop-and-create` is set. No Spring `@DataJpaTest` setup, no Flyway migration needed for dev data — just drop an SQL file in `src/main/resources`.

    `nextval('MenuItem_SEQ')` uses the auto-generated Hibernate sequence for the `id` column.

---

## Step 6 — Test It

Save all files. Quarkus live-reloads automatically. Now open `http://localhost:8080/q/swagger-ui` and:

**Try `GET /menu`:**

Execute it. You should now see the 3 seed items returned from the **database** — not the hardcoded list.

Check your `quarkus dev` terminal — you'll see the SQL that Hibernate executed:

```sql
Hibernate: select m1_0.id,m1_0.description,m1_0.name,m1_0.price from MenuItem m1_0
```

**Try `POST /menu`:**

Add a new item:

```json
{
  "name": "Oat Milk Latte",
  "description": "Espresso with creamy oat milk",
  "price": 4.75
}
```

Check the terminal — you'll see the `INSERT` statement. Execute `GET /menu` again and the new item is there, with a generated `id`.

---

## Step 7 — Dev UI Database Browser

Quarkus Dev UI has a live database browser. Open:

```
http://localhost:8080/q/dev-ui
```

Navigate to the database browser in two steps:

1. Find the **Hibernate ORM** card and click **Entity Types**
2. In the entity list, click the **`MenuItem`** table name link — this opens a live SQL query browser against your H2 database

Run the following query:

```sql
SELECT * FROM MenuItem;
```

You'll see your rows — including any you just `POST`ed — live in the browser with no external tooling needed.

!!! tip "Can't find the Hibernate ORM card?"
    The Dev UI shows only the extensions active in your project. If the card isn't visible, scroll down — cards are ordered alphabetically. You can also go directly to:
    ```
    http://localhost:8080/q/dev-ui/io.quarkus.quarkus-hibernate-orm/persistence-units
    ```

!!! tip "Active Record vs Repository"
    Panache supports two patterns:

    **Active Record** (this lab): DB methods live on the entity class itself.
    ```java
    MenuItem.listAll();
    item.persist();
    ```

    **Repository**: a separate `@ApplicationScoped` class extends `PanacheRepository<MenuItem>`.
    ```java
    @ApplicationScoped
    public class MenuItemRepository implements PanacheRepository<MenuItem> { }
    ```

    Both are equivalent. Active Record is more concise; Repository suits teams that prefer a strict separation of concerns. Choose either — the Quarkus guide covers both.

---

## Summary

| What | How |
|------|-----|
| ✅ Added JPA persistence | `@Entity`, `extends PanacheEntity` |
| ✅ Zero-boilerplate DB access | `MenuItem.listAll()`, `item.persist()` |
| ✅ Schema auto-created | `hibernate-orm.database.generation=drop-and-create` |
| ✅ Seeded with `import.sql` | Runs automatically on startup |
| ✅ Viewed live DB in browser | Dev UI datasource panel |

!!! tip "Stuck or fell behind?"
    The complete solution is in `labs/lab2-panache/solution/`. Run it with:

    === "Quarkus CLI"
        ```bash
        cd labs/lab2-panache/solution
        quarkus dev
        ```
    === "Maven"
        ```bash
        cd labs/lab2-panache/solution
        ./mvnw quarkus:dev
        ```

---

[← Lab 1: First REST API](lab1-rest.md){ .md-button }
[→ Lab 3: Config & Health](lab3-config-health.md){ .md-button .md-button--primary }
