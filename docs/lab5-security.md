# Lab 5: OIDC Security with Keycloak DevServices

**Duration:** 7 minutes &nbsp;|&nbsp; **Project:** `menu-service` (continued)

!!! info "What you'll build"
    Protect the `POST /menu` endpoint so only authenticated users can add items, and restrict a new admin endpoint to users with the `barista` role. Keycloak starts automatically — no realm setup, no import files, no `docker run`.

!!! warning "Docker or Podman required"
    Keycloak runs as a DevServices container. Confirm your container runtime is running before starting:
    ```bash
    docker ps   # or: podman ps
    ```

**New extension added in this lab:**

| Extension | Purpose |
|-----------|---------|
| `quarkus-oidc` | OpenID Connect / OAuth2 bearer token authentication |

---

## Background: Why OIDC?

!!! note "What is OIDC?"
    OpenID Connect (OIDC) is the industry standard for API security. Instead of managing passwords in your service, you delegate authentication to an **identity provider** (Keycloak, Auth0, Okta, etc.). Your API receives a **JWT bearer token** in the `Authorization` header and validates it cryptographically — no database lookup needed.

    This is the pattern used by virtually every production microservices deployment. Quarkus makes it a one-extension, three-line-config exercise.

---

## Step 1 — Add the OIDC Extension

In a terminal inside the `menu-service` directory:

=== "Quarkus CLI"

    ```bash
    quarkus ext add oidc
    ```

=== "Maven"

    ```bash
    ./mvnw quarkus:add-extension -Dextensions="oidc"
    ```

Open `src/main/resources/application.properties` and add:

```properties title="application.properties"
# Tell Quarkus this is a bearer-token REST API (not a web app with login pages)
quarkus.oidc.application-type=service
```

!!! note "DevServices magic — no auth-server-url needed"
    When Quarkus sees `quarkus-oidc` on the classpath and **no `quarkus.oidc.auth-server-url`** is configured, it automatically starts a **Keycloak** container using DevServices. It creates:

    - A realm named `quarkus`
    - A client with the correct settings
    - Two test users: `alice` (role: `user`) and `bob` (role: `admin`)

    You get a fully working identity provider with zero manual setup.

---

## Step 2 — Protect the Endpoints

Update `src/main/java/org/coffee/MenuResource.java` to add security annotations:

```java
package org.coffee;

import io.quarkus.security.Authenticated;           // (1)
import jakarta.annotation.security.RolesAllowed;    // (2)
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
    public List<MenuItem> list() {        // public — no auth required
        return MenuItem.listAll();
    }

    @POST
    @Authenticated                        // (1) any valid JWT token accepted
    @Transactional
    public Response add(MenuItem item) {
        item.persist();
        return Response.status(Response.Status.CREATED).entity(item).build();
    }

    @POST
    @Path("/admin")
    @RolesAllowed("barista")              // (2) only users with the 'barista' role
    @Transactional
    public Response addAsBarista(MenuItem item) {
        item.persist();
        return Response.status(Response.Status.CREATED).entity(item).build();
    }
}
```

1. `@Authenticated` — any request with a valid, unexpired JWT is allowed. Invalid or missing token → HTTP 401.
2. `@RolesAllowed("barista")` — the JWT must contain a `barista` role claim. Wrong role → HTTP 403.

!!! note "Where do the imports come from?"
    - `@Authenticated` — `io.quarkus.security.Authenticated` (Quarkus-specific, on classpath via `quarkus-oidc`)
    - `@RolesAllowed` — `jakarta.annotation.security.RolesAllowed` (standard Jakarta EE annotation)

---

## Step 3 — Start Dev Mode and Wait for Keycloak

Save all files. If `quarkus dev` is already running it will live-reload. Watch the terminal for:

```
Dev Services for Keycloak started.
```

!!! warning "Keycloak takes ~15 seconds on first start"
    The first time Keycloak starts, Docker pulls the image and boots the server. This takes about 15 seconds. Subsequent starts (same Docker session) are much faster because the image is cached.

    If you see `Connection refused` errors in the terminal — just wait. Keycloak is still starting.

---

## Step 4 — Get a Token from Dev UI

Open:

```
http://localhost:8080/q/dev-ui
```

Find the **OpenID Connect** card and click **"Login into Single Page Application"**.

You'll see a login page served by Keycloak. Log in as:

- **Username:** `alice`
- **Password:** `alice`

After login, the Dev UI shows the **Access Token** for `alice`. Click the copy icon to copy it to your clipboard.

!!! note "What just happened?"
    The Dev UI has a built-in OIDC test client. It performed the OAuth2 Authorization Code flow against the DevServices Keycloak, obtained a JWT access token, and displayed it for you — without writing any test code.

---

## Step 5 — Test the Security

Open a terminal and test all three scenarios with `curl`. Replace `<TOKEN>` with the token you copied.

**No token → 401 Unauthorized:**

```bash
curl -i -X POST http://localhost:8080/menu \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","description":"test","price":1.0}'
```

```
HTTP/1.1 401 Unauthorized
```

**Valid token (alice) → 201 Created:**

```bash
curl -i -X POST http://localhost:8080/menu \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"name":"Oat Latte","description":"Creamy oat milk latte","price":4.50}'
```

```
HTTP/1.1 201 Created
{"id":4,"name":"Oat Latte","description":"Creamy oat milk latte","price":4.5}
```

**Valid token but wrong role → 403 Forbidden:**

```bash
curl -i -X POST http://localhost:8080/menu/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"name":"Barista Special","description":"Secret recipe","price":6.00}'
```

```
HTTP/1.1 403 Forbidden
```

!!! note "401 vs 403 — what's the difference?"
    - **401 Unauthorized** — no token, or the token is invalid/expired. The client needs to authenticate.
    - **403 Forbidden** — the token is valid and the user is authenticated, but they don't have the required role. The client is authenticated but not authorised.

    These are different problems requiring different responses.

---

## Step 6 — What DevServices Created

Back in the Dev UI OpenID Connect panel, you can inspect what Keycloak auto-configured:

| Setting | Value |
|---------|-------|
| Realm | `quarkus` |
| Client ID | `quarkus-app` |
| Test user 1 | `alice` / `alice` — roles: `user` |
| Test user 2 | `bob` / `bob` — roles: `admin` |

!!! tip "What happens in production?"
    You set `quarkus.oidc.auth-server-url` to your real Keycloak or any OIDC provider (Auth0, Okta, Azure AD). The security annotations (`@Authenticated`, `@RolesAllowed`) stay exactly the same — no code changes needed.

    ```properties
    # Production config (in application.properties or environment variable)
    quarkus.oidc.auth-server-url=https://keycloak.mycompany.com/realms/production
    quarkus.oidc.client-id=menu-service
    quarkus.oidc.credentials.secret=${OIDC_CLIENT_SECRET}
    ```

---

## Summary

| What | How |
|------|-----|
| ✅ Added OIDC security | `quarkus ext add oidc` + one config line |
| ✅ Protected endpoint | `@Authenticated` on `POST /menu` |
| ✅ Role-based access control | `@RolesAllowed("barista")` on admin endpoint |
| ✅ Full Keycloak, zero setup | DevServices auto-started it |
| ✅ Got a real JWT | From Dev UI login flow |
| ✅ Verified 401 / 403 / 201 | With `curl` |

!!! tip "Stuck or fell behind?"
    The complete solution is in `labs/lab5-security/solution/`. Run it with:
    ```bash
    cd labs/lab5-security/solution
    quarkus dev
    ```

---

[← Lab 4: Kafka Messaging](lab4-kafka.md){ .md-button }
[→ Lab 6: Fault Tolerance](lab6-fault-tolerance.md){ .md-button .md-button--primary }
