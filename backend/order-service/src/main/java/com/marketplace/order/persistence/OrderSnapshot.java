package com.marketplace.order.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderSnapshot(
        UUID orderId,
        String status,
        String customerId,
        List<StoredOrderItem> items,
        Instant createdAt
) {
}
