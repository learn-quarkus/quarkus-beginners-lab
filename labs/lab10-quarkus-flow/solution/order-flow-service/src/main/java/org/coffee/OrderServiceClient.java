package org.coffee;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Type-safe REST client for order-service (Lab 4, port 8081).
 * Configured via: quarkus.rest-client.order-service.url=http://localhost:8081
 */
@RegisterRestClient(configKey = "order-service")
@Path("/orders")
public interface OrderServiceClient {

    @POST
    Response placeOrder(Order order);
}
