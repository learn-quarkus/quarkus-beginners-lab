package org.coffee;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST facade for the order workflow.
 *
 * <p>POST /flow/order  — triggers the workflow; returns orderId + status.
 * <p>POST /flow/approve/{orderId} — confirms a PENDING_APPROVAL order (barista action).
 */
@Path("/flow")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderFlowResource {

    @Inject
    OrderFlowWorkflow orderFlow;

    /**
     * Tracks orders that are awaiting barista approval.
     * Key: orderId, Value: the workflow output (for audit / status lookup).
     */
    private final ConcurrentHashMap<String, OrderState> pendingOrders =
        new ConcurrentHashMap<>();

    /**
     * Place an order and run it through the approval workflow.
     *
     * <p>Request body: {@link Order} JSON with itemName, quantity, customerId, totalPrice.
     * <p>Response 200: {@code { "orderId": "...", "status": "CONFIRMED", ... }}
     * <p>Response 202: {@code { "orderId": "...", "status": "PENDING_APPROVAL", ... }}
     */
    @POST
    @Path("/order")
    public Uni<Response> placeOrder(Order order) {
        return orderFlow
            .startInstance(order)                              // reactive — non-blocking
            .onItem().transform(model -> {
                OrderState result = model.as(OrderState.class).orElseThrow();

                if ("PENDING_APPROVAL".equals(result.status)) {
                    pendingOrders.put(result.orderId, result);
                    return Response.accepted(result).build();  // 202
                }

                return Response.ok(result).build();            // 200
            });
    }

    /**
     * Approve a pending high-value order.
     *
     * <p>Called by the barista after reviewing the order.
     * Removes the order from the pending map and returns the confirmed result.
     *
     * <p>Response 200: {@code { "orderId": "...", "status": "CONFIRMED", ... }}
     * <p>Response 404: order not found or already processed.
     */
    @POST
    @Path("/approve/{orderId}")
    public Response approveOrder(@PathParam("orderId") String orderId) {
        OrderState pending = pendingOrders.remove(orderId);
        if (pending == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Order " + orderId + " not found or already processed"))
                .build();
        }

        pending.status = "CONFIRMED";
        return Response.ok(pending).build();
    }
}
