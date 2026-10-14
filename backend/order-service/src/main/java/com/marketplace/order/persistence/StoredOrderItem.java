package com.marketplace.order.persistence;

import java.util.UUID;

public record StoredOrderItem(UUID productId, int quantity) {
}
