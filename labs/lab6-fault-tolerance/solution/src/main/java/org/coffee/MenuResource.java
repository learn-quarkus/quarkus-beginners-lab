package org.coffee;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.List;

@Path("/menu")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MenuResource {

    @Inject
    PricingService pricingService;

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
    @Path("/{id}/price")
    @Produces(MediaType.TEXT_PLAIN)
    public BigDecimal getPrice(@PathParam("id") Long id) {
        return pricingService.getPrice(id);
    }
}
