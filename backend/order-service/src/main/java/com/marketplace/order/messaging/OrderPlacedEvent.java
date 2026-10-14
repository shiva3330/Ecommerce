package com.marketplace.order.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderPlacedEvent(
        UUID eventId,
        UUID orderId,
        String customerId,
        List<Item> items,
        Instant occurredAt
) {

    /**
     * Kept for source compatibility with the original event port. New durable
     * events always use the constructor that supplies an event id explicitly.
     */
    public OrderPlacedEvent(UUID orderId, String customerId, List<Item> items, Instant occurredAt) {
        this(UUID.randomUUID(), orderId, customerId, items, occurredAt);
    }

    public record Item(UUID productId, int quantity) {
    }
}
