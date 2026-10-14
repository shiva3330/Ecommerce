package com.marketplace.order.persistence;

import java.util.UUID;

public record OutboxMessage(
        UUID eventId,
        UUID aggregateId,
        String topic,
        String eventKey,
        String payload,
        int attempts
) {
}
