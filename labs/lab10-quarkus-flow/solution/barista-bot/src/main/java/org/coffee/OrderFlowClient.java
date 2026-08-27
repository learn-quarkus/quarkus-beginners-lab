package org.coffee;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client that calls order-flow-service (port 8082).
 * Configured via: quarkus.rest-client.order-flow-service.url=http://localhost:8082
 */
@RegisterRestClient(configKey = "order-flow-service")
@Path("/flow")
public interface OrderFlowClient {

    @POST
    @Path("/order")
    OrderResult placeOrder(OrderRequest request);
}
