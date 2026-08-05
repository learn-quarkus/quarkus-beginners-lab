package org.coffee;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import jakarta.transaction.Transactional;

@Liveness
@ApplicationScoped
public class CoffeeShopHealthCheck implements HealthCheck {

    @Override
    @Transactional
    public HealthCheckResponse call() {
        long count = MenuItem.count();
        if (count > 0) {
            return HealthCheckResponse.named("coffee-menu")
                .up()
                .withData("itemCount", count)
                .build();
        } else {
            return HealthCheckResponse.named("coffee-menu")
                .down()
                .withData("reason", "Menu is empty — no items loaded")
                .build();
        }
    }
}
