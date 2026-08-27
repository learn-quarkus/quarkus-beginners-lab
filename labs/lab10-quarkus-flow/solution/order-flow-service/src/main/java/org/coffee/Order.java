package org.coffee;

/**
 * Mirrors the Order POJO from order-service (Lab 4), extended with totalPrice
 * so order-flow-service can apply the approval threshold check.
 */
public class Order {

    public String itemName;
    public int quantity;
    public String customerId;
    public double totalPrice;

    public Order() {}

    public Order(String itemName, int quantity, String customerId, double totalPrice) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.customerId = customerId;
        this.totalPrice = totalPrice;
    }
}
