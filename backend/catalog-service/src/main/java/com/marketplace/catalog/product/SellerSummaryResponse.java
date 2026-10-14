package com.marketplace.catalog.product;

import java.math.BigDecimal;
import java.util.UUID;

public record SellerSummaryResponse(
        UUID id,
        String name,
        BigDecimal rating
) {
}
