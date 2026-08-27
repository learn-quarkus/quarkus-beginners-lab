package org.coffee;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Verifies the Quarkus Flow workflow branches correctly around the approval
 * threshold. The order-service REST client is mocked so no network call is made.
 */
@QuarkusTest
class OrderFlowWorkflowTest {

    @Inject
    OrderFlowWorkflow orderFlow;

    @InjectMock
    @RestClient
    OrderServiceClient orderServiceClient;

    private OrderState run(Order order) {
        when(orderServiceClient.placeOrder(order)).thenReturn(Response.accepted().build());
        return orderFlow.startInstance(order)
                .await().indefinitely()
                .as(OrderState.class)
                .orElseThrow();
    }

    @Test
    void lowValueOrderIsConfirmed() {
        OrderState result = run(new Order("Espresso", 1, "alice", 3.50));
        assertEquals("CONFIRMED", result.status);
        assertNotNull(result.orderId);
        assertEquals("Espresso", result.itemName);
    }

    @Test
    void highValueOrderRequiresApproval() {
        OrderState result = run(new Order("Party Box", 10, "bob", 42.00));
        assertEquals("PENDING_APPROVAL", result.status);
        assertNotNull(result.orderId);
    }

    @Test
    void orderAtThresholdRequiresApproval() {
        // Default threshold is 15.0 — exactly at the boundary must require approval.
        OrderState result = run(new Order("Cortado", 5, "carol", 15.00));
        assertEquals("PENDING_APPROVAL", result.status);
    }
}
