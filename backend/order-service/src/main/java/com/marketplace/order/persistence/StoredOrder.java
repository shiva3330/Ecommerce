package com.marketplace.order.persistence;

import java.time.Instant;
import java.util.UUID;

public record StoredOrder(
        UUID orderId,
        String customerId,
        String status,
        String requestHash,
        Instant createdAt
) {
}
