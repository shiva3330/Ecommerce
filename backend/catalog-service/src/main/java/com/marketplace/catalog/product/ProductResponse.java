package com.marketplace.catalog.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        String category,
        BigDecimal price,
        String currency,
        BigDecimal rating,
        int reviewCount,
        int inventory,
        SellerSummaryResponse seller,
        List<String> tags,
        boolean featured
) {

    /**
     * Keeps the original response construction usable for callers that only
     * provide the fields that existed before the storefront contract.
     */
    public ProductResponse(UUID id, String name, String description, BigDecimal price, String currency) {
        this(
                id,
                name,
                description,
                "Uncategorized",
                price,
                currency,
                BigDecimal.ZERO,
                0,
                0,
                new SellerSummaryResponse(
                        UUID.fromString("00000000-0000-4000-8000-000000000001"),
                        "Unknown Seller",
                        BigDecimal.ZERO
                ),
                List.of(),
                false
        );
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription() == null ? "" : product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getCurrency(),
                product.getRating(),
                product.getReviewCount(),
                product.getInventory(),
                new SellerSummaryResponse(
                        product.getSellerId(),
                        product.getSellerName(),
                        product.getSellerRating()
                ),
                List.copyOf(product.getTags()),
                product.isFeatured()
        );
    }
}
