package org.coffee;

public class Order {
    public String itemName;
    public int quantity;
    public String customerId;

    public Order() {}

    public Order(String itemName, int quantity, String customerId) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.customerId = customerId;
    }
}
