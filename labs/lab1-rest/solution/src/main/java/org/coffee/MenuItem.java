package org.coffee;

public class MenuItem {
    public String name;
    public String description;
    public double price;

    public MenuItem() {}

    public MenuItem(String name, String description, double price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }
}
