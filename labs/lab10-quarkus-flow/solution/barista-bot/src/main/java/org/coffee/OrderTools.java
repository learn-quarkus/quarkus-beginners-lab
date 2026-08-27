package org.coffee;

import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * LangChain4j @Tool bean — called by the LLM when a user asks to place an order.
 *
 * <p>The LLM first looks up the item price via the MCP {@code getItemPrice} tool,
 * then calls {@link #placeOrder} with the derived totalPrice.
 */
@ApplicationScoped
public class OrderTools {

    @RestClient
    @Inject
    OrderFlowClient orderFlowClient;

    @Tool("Place a coffee order. Returns the order ID and status. " +
          "Use getItemPrice first to calculate totalPrice = price × quantity.")
    public String placeOrder(
            String item_name,
            int quantity,
            String customer_id,
            double total_price) {

        OrderResult result = orderFlowClient.placeOrder(
            new OrderRequest(item_name, quantity, customer_id, total_price));

        if ("PENDING_APPROVAL".equals(result.status)) {
            return "Your order has been received but requires barista approval " +
                   "before it can be prepared (total $" + String.format("%.2f", total_price) +
                   " exceeds our express limit). Order ID: " + result.orderId +
                   ". A barista will confirm it shortly.";
        }

        return "Your order is confirmed! ☕ " + quantity + "× " + item_name +
               " — Order #" + result.orderId + ". Total: $" + String.format("%.2f", total_price);
    }
}
