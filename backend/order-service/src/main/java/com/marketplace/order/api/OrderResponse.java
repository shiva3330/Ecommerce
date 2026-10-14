package com.marketplace.order.api;

import java.util.UUID;

public record OrderResponse(UUID orderId, String status) {
}
