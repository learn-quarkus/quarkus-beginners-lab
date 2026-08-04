package org.coffee;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OrderConsumer {

    private static final Logger LOG = Logger.getLogger(OrderConsumer.class);

    @Incoming("coffee-orders")
    public void onOrder(String orderJson) {
        LOG.infof("☕ New order received: %s", orderJson);
    }
}
