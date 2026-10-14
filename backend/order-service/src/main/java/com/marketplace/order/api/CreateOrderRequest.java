package com.marketplace.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "customerId is required")
        @Size(max = 100, message = "customerId must be at most 100 characters")
        String customerId,

        @NotNull(message = "items is required")
        @NotEmpty(message = "at least one item is required")
        @Size(max = 100, message = "an order cannot contain more than 100 items")
        List<@NotNull(message = "order item must not be null") @Valid OrderLineRequest> items
) {
}
