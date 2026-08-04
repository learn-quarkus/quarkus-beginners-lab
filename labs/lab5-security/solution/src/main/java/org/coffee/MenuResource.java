package org.coffee;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
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
        return MenuItem.listAll();
    }

    @POST
    @Authenticated              // Any valid JWT token is accepted
    @Transactional
    public Response add(MenuItem item) {
        item.persist();
        return Response.status(Response.Status.CREATED).entity(item).build();
    }

    @POST
    @Path("/admin")
    @RolesAllowed("admin")      // Only users with the 'admin' role
    @Transactional
    public Response addAsAdmin(MenuItem item) {
        item.persist();
        return Response.status(Response.Status.CREATED).entity(item).build();
    }
}
