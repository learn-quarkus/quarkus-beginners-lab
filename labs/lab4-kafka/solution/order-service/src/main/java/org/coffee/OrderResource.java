package org.coffee;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Broadcast;
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
    @Channel("coffee-orders")
    Emitter<String> emitter;

    @Inject
    ObjectMapper objectMapper;

    @POST
    public Response placeOrder(Order order) {
        try {
            String json = objectMapper.writeValueAsString(order);
            emitter.send(json);
            return Response.status(Response.Status.ACCEPTED).entity(order).build();
        } catch (JsonProcessingException e) {
            return Response.serverError().entity("Failed to serialise order").build();
        }
    }
}
