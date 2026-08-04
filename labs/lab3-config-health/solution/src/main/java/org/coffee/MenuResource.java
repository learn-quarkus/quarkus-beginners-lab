package org.coffee;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.List;

@Path("/menu")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MenuResource {

    @ConfigProperty(name = "coffee.shop.name", defaultValue = "The Quarkus Cafe")
    String shopName;

    @GET
    public List<MenuItem> list() {
        return MenuItem.listAll();
    }

    @POST
    @Transactional
    public Response add(MenuItem item) {
        item.persist();
        return Response.status(Response.Status.CREATED).entity(item).build();
    }

    @GET
    @Path("/info")
    public String info() {
        return "Welcome to " + shopName + "! We have " + MenuItem.count() + " items on the menu.";
    }
}
