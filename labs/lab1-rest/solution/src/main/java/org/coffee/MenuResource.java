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

    // In-memory list for Lab 1 — replaced with DB in Lab 2
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
