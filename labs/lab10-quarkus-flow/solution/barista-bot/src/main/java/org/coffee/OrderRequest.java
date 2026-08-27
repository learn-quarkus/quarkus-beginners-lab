package org.coffee;

/**
 * Request body for POST /flow/order on order-flow-service.
 */
public class OrderRequest {

    public String itemName;
    public int quantity;
    public String customerId;
    public double totalPrice;

    public OrderRequest() {}

    public OrderRequest(String itemName, int quantity, String customerId, double totalPrice) {
        this.itemName   = itemName;
        this.quantity   = quantity;
        this.customerId = customerId;
        this.totalPrice = totalPrice;
    }
}
