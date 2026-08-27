package org.coffee;

/**
 * Response from POST /flow/order on order-flow-service.
 */
public class OrderResult {

    public String orderId;
    public String status;   // "CONFIRMED" or "PENDING_APPROVAL"

    public OrderResult() {}

    public OrderResult(String orderId, String status) {
        this.orderId = orderId;
        this.status  = status;
    }

    @Override
    public String toString() {
        return "Order #" + orderId + " — " + status;
    }
}
