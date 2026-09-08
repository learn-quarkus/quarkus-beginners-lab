package org.coffee;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * REST facade for the order workflow.
 *
 * <p>POST /flow/order             — triggers the workflow; returns orderId + status.
 * <p>POST /flow/approve/{orderId} — confirms a PENDING_APPROVAL order (API / curl).
 * <p>GET  /admin                  — barista approval UI (see {@link AdminResource}).
 */
@Path("/flow")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderFlowResource {

    @Inject
    OrderFlowWorkflow orderFlow;

    @Inject
    PendingOrdersStore store;

    /**
     * Place an order and run it through the approval workflow.
     *
     * <p>Response 200: {@code { "orderId": "...", "status": "CONFIRMED", ... }}
     * <p>Response 202: {@code { "orderId": "...", "status": "PENDING_APPROVAL", ... }}
     */
    @POST
    @Path("/order")
    @Blocking
    public Uni<Response> placeOrder(Order order) {
        return orderFlow
            .startInstance(order)
            .onItem().transform(model -> {
                OrderState result = model.as(OrderState.class).orElseThrow();

                if ("PENDING_APPROVAL".equals(result.status)) {
                    store.add(result);
                    return Response.accepted(result).build();  // 202
                }

                return Response.ok(result).build();            // 200
            });
    }

    /**
     * Check the current status of an order.
     *
     * <p>Response 200: {@code { "orderId": "...", "status": "PENDING_APPROVAL" }}  — still waiting
     * <p>Response 200: {@code { "orderId": "...", "status": "CONFIRMED" }}         — approved or was never held
     */
    @GET
    @Path("/status/{orderId}")
    public Response getOrderStatus(@PathParam("orderId") String orderId) {
        return Response.ok(Map.of("orderId", orderId, "status", store.status(orderId))).build();
    }

    /**
     * Approve a pending order via API (curl / programmatic).
     * The admin UI at {@code /admin} does the same via a browser form.
     *
     * <p>Response 200: {@code { "orderId": "...", "status": "CONFIRMED", ... }}
     * <p>Response 404: order not found or already processed.
     */
    @POST
    @Path("/approve/{orderId}")
    public Response approveOrder(@PathParam("orderId") String orderId) {
        OrderState pending = store.approve(orderId);
        if (pending == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Order " + orderId + " not found or already processed"))
                .build();
        }

        pending.status = "CONFIRMED";
        return Response.ok(pending).build();
    }
}
