package com.marketplace.order.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailsResponse(
        UUID orderId,
        String status,
        String customerId,
        List<OrderLineResponse> items,
        Instant createdAt
) {

    public record OrderLineResponse(UUID productId, int quantity) {
    }
}
