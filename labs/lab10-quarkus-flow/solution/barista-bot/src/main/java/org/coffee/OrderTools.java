package org.coffee;

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class OrderTools {

    @RestClient
    @Inject
    OrderFlowClient orderFlowClient;

    @Tool("Place a coffee order. Returns the order ID and status. "
        + "Use getItemPrice first to calculate totalPrice = price × quantity.")
    public String placeOrder(
            String item_name,
            int    quantity,
            String customer_id,
            double total_price) {

        OrderResult result = orderFlowClient.placeOrder(
            new OrderRequest(item_name, quantity, customer_id, total_price));

        if ("PENDING_APPROVAL".equals(result.status)) {
            return "Order received but requires barista approval (total $"
                   + String.format("%.2f", total_price) + " exceeds the express limit). "
                   + "Order ID: " + result.orderId + ". "
                   + "Tell the customer their order is pending and they can ask you to check "
                   + "the status at any time using the order ID.";
        }
        return "Order confirmed! ☕ " + quantity + "× " + item_name
               + " — Order #" + result.orderId
               + ". Total: $" + String.format("%.2f", total_price);
    }

    @Tool("Check the current status of an order by its order ID. "
        + "Call this when the customer asks about their order status or whether it has been approved.")
    public String getOrderStatus(String order_id) {
        OrderResult result = orderFlowClient.getOrderStatus(order_id);

        if ("PENDING_APPROVAL".equals(result.status)) {
            return "Order #" + order_id + " is still awaiting barista approval.";
        }
        return "Order #" + order_id + " has been confirmed! ☕ Your order is on its way.";
    }
}
