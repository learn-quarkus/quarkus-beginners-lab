package org.coffee;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class PricingService {

    private static final Logger LOG = Logger.getLogger(PricingService.class);

    @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Fallback(fallbackMethod = "defaultPrice")
    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    public BigDecimal getPrice(Long itemId) {
        LOG.debugf("Fetching price for item %d ...", itemId);
        // Simulate a flaky external pricing service (50% failure rate)
        if (Math.random() < 0.5) {
            LOG.debugf("Simulated failure for item %d — will retry", itemId);
            throw new RuntimeException("External pricing service unavailable");
        }
        LOG.debugf("Got price for item %d", itemId);
        return BigDecimal.valueOf(3.50 + (itemId % 3));
    }

    // Called automatically when all retries are exhausted
    public BigDecimal defaultPrice(Long itemId) {
        LOG.debugf("Returning fallback price $4.99 for item %d", itemId);
        return BigDecimal.valueOf(4.99);
    }
}
