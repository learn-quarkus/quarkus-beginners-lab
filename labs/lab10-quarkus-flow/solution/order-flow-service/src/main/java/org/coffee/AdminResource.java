package org.coffee;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

/**
 * Barista admin UI — list and approve pending high-value orders.
 *
 * <p>GET  /admin           — renders the admin page (admin.html Qute template)
 * <p>POST /admin/approve/{orderId} — approves the order and redirects back
 */
@Path("/admin")
public class AdminResource {

    @Inject
    Template admin;

    @Inject
    PendingOrdersStore store;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index(
        @jakarta.ws.rs.QueryParam("approved") String approved,
        @jakarta.ws.rs.QueryParam("error") String error) {
      return admin
          .data("orders",         store.allPending())
          .data("approvedOrders", store.allApproved())
          .data("approved",       approved)
          .data("error",          error);
    }
  
    @POST
    @Path("/approve/{orderId}")
    public Response approve(@PathParam("orderId") String orderId) {
      OrderState order = store.approve(orderId);
      if (order == null) {
        return Response.seeOther(URI.create("/admin?error=" + orderId)).build();
      }
      order.status = "CONFIRMED";
      return Response.seeOther(URI.create("/admin?approved=" + orderId)).build();
    }
}
