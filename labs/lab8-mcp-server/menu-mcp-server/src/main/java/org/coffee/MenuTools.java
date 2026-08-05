package org.coffee;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class MenuTools {

    private static final List<Map<String, Object>> MENU = List.of(
        Map.of("name", "Espresso",   "price", 2.50, "milk", List.of()),
        Map.of("name", "Cappuccino", "price", 3.75, "milk", List.of("whole", "oat")),
        Map.of("name", "Flat White", "price", 4.00, "milk", List.of("whole")),
        Map.of("name", "Latte",      "price", 4.25, "milk", List.of("whole", "oat", "almond", "soy")),
        Map.of("name", "Americano",  "price", 3.00, "milk", List.of()),
        Map.of("name", "Cold Brew",  "price", 4.00, "milk", List.of("whole", "oat")),
        Map.of("name", "Iced Latte", "price", 4.50, "milk", List.of("whole", "oat", "almond"))
    );

    @Tool(description = "Get all menu items with their name, price, and available milk options")
    public String getMenuItems() {
        return MENU.stream()
            .map(item -> String.format("%-12s $%.2f  milk: %s",
                item.get("name"), item.get("price"),
                ((List<?>) item.get("milk")).isEmpty() ? "none" : item.get("milk")))
            .collect(Collectors.joining("\n"));
    }

    @Tool(description = "Get the price of a specific menu item by name")
    public String getItemPrice(
            @ToolArg(description = "Name of the menu item, e.g. Espresso") String name) {
        return MENU.stream()
            .filter(item -> item.get("name").toString().equalsIgnoreCase(name))
            .map(item -> String.format("%s costs $%.2f", item.get("name"), item.get("price")))
            .findFirst()
            .orElse("Item '" + name + "' not found on the menu.");
    }

    @Tool(description = "Get menu items available with a specific milk option")
    public String getItemsByMilkOption(
            @ToolArg(description = "Milk type: whole, oat, almond, or soy") String milk) {
        var matches = MENU.stream()
            .filter(item -> ((List<?>) item.get("milk")).stream()
                .anyMatch(m -> m.toString().equalsIgnoreCase(milk)))
            .map(item -> String.format("%s ($%.2f)", item.get("name"), item.get("price")))
            .collect(Collectors.joining(", "));
        return matches.isEmpty()
            ? "No items available with " + milk + " milk."
            : "Items with " + milk + " milk: " + matches;
    }
}
