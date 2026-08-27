package org.coffee;

/**
 * Typed workflow context object — passed between tasks in {@link OrderFlowWorkflow}.
 *
 * <p>Using a POJO rather than a raw Map lets Quarkus Flow deserialise the
 * workflow state for you: {@code function()} tasks declare their input and
 * output types explicitly, and the engine handles JSON conversion automatically.
 */
public class OrderState {

    public String orderId;
    public String itemName;
    public int    quantity;
    public String customerId;
    public double totalPrice;
    public String status; // "CONFIRMED" or "PENDING_APPROVAL"

    public OrderState() {}
}
