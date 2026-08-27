package org.coffee;

import io.quarkiverse.flow.Flow;
import io.serverlessworkflow.api.types.FlowDirectiveEnum;
import io.serverlessworkflow.api.types.Workflow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.UUID;

import static io.quarkiverse.flow.dsl.FlowDSL.function;
import static io.quarkiverse.flow.dsl.FlowDSL.switchWhenOrElse;
import static io.quarkiverse.flow.dsl.FlowWorkflowBuilder.workflow;

/**
 * Quarkus Flow workflow for placing a coffee order with a conditional HITL gate.
 *
 * <p>The workflow runs three tasks:
 * <ol>
 *   <li>{@code placeOrder} — generate an orderId, call order-service to record the
 *       order, and carry the order details forward as an {@link OrderState}.</li>
 *   <li>a switch — compare {@code totalPrice} against the configurable approval
 *       threshold and branch:
 *       <ul>
 *         <li>at or above threshold → {@code requireApproval} (status = PENDING_APPROVAL)</li>
 *         <li>below threshold → {@code confirmOrder} (status = CONFIRMED)</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>Triggered by {@link OrderFlowResource#placeOrder}.
 * High-value orders are resumed by {@link OrderFlowResource#approveOrder}.
 */
@ApplicationScoped
public class OrderFlowWorkflow extends Flow {

    @RestClient
    OrderServiceClient orderServiceClient;

    @ConfigProperty(name = "coffee.approval.threshold", defaultValue = "15.0")
    double threshold;

    @Override
    public Workflow descriptor() {
        // Capture the configured threshold as a local so the switch predicate
        // closes over the value, not the CDI bean.
        final double approvalThreshold = threshold;

        return workflow("order-flow")
            .tasks(
                // Step 1: generate an orderId and call order-service
                function("placeOrder", (Order order) -> {
                    String orderId = UUID.randomUUID().toString().substring(0, 8);

                    // Call Lab 4's order-service to record the order
                    try (Response resp = orderServiceClient.placeOrder(order)) {
                        // order-service returns 202; we proceed regardless
                    }

                    OrderState state = new OrderState();
                    state.orderId    = orderId;
                    state.itemName   = order.itemName;
                    state.quantity   = order.quantity;
                    state.customerId = order.customerId;
                    state.totalPrice = order.totalPrice;
                    return state;
                }, Order.class),

                // Step 2: branch on the order value
                switchWhenOrElse(
                    (OrderState state) -> state.totalPrice >= approvalThreshold,
                    "requireApproval",   // high-value path
                    "confirmOrder",      // low-value path
                    OrderState.class),

                // High-value path — barista must approve. End here so we don't
                // fall through into confirmOrder.
                function("requireApproval", (OrderState state) -> {
                    state.status = "PENDING_APPROVAL";
                    return state;
                }, OrderState.class).then(FlowDirectiveEnum.END),

                // Low-value path — confirm straight away
                function("confirmOrder", (OrderState state) -> {
                    state.status = "CONFIRMED";
                    return state;
                }, OrderState.class)
            )
            .build();
    }
}
