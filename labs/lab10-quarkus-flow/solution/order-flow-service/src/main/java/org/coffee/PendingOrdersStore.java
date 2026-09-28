package org.coffee;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Shared in-memory store for orders awaiting barista approval and approval history.
 *
 * <p>Injected by both {@link OrderFlowResource} (writes) and
 * {@link AdminResource} (reads + approvals) so both endpoints
 * operate on the same collections without coupling the two resources directly.
 */
@ApplicationScoped
public class PendingOrdersStore {

    private final ConcurrentHashMap<String, OrderState> pending  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, OrderState> completed = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<OrderState>      approved = new CopyOnWriteArrayList<>();

    public void add(OrderState order) {
        pending.put(order.orderId, order);
    }

    /** Return the order without removing it, or null if not found / already approved. */
    public OrderState get(String orderId) {
        return pending.get(orderId);
    }

    /** Remove from pending, record in history, and return the order — or null if not found. */
    public OrderState approve(String orderId) {
        OrderState order = pending.remove(orderId);
        if (order != null) {
            order.status = "CONFIRMED";
            completed.put(orderId, order);
            approved.add(0, order); // newest first
        }
        return order;
    }

    /** Return the current status, including orders approved through the admin UI. */
    public String status(String orderId) {
        OrderState waiting = pending.get(orderId);
        if (waiting != null) {
            return waiting.status;
        }
        OrderState finished = completed.get(orderId);
        return finished == null ? "CONFIRMED" : finished.status;
    }

    public Collection<OrderState> allPending() {
        return Collections.unmodifiableCollection(pending.values());
    }

    public List<OrderState> allApproved() {
        return Collections.unmodifiableList(approved);
    }

    public boolean isEmpty() {
        return pending.isEmpty();
    }
}
