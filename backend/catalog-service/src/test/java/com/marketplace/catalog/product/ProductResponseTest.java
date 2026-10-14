package com.marketplace.catalog.product;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductResponseTest {

    @Test
    void mapsTheEntityFieldsNeededByTheStorefront() {
        UUID sellerId = UUID.fromString("10000000-0000-4000-8000-000000000001");
        Product product = new Product(
                "Handwoven Linen Throw",
                "A textured layer",
                "Home",
                new BigDecimal("86.0000"),
                "USD",
                new BigDecimal("4.90"),
                128,
                14,
                sellerId,
                "Harbor & Loom",
                new BigDecimal("4.90"),
                List.of("linen", "giftable"),
                true
        );

        ProductResponse response = ProductResponse.from(product);

        assertThat(response.name()).isEqualTo("Handwoven Linen Throw");
        assertThat(response.description()).isEqualTo("A textured layer");
        assertThat(response.category()).isEqualTo("Home");
        assertThat(response.price()).isEqualByComparingTo("86.0000");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.rating()).isEqualByComparingTo("4.90");
        assertThat(response.reviewCount()).isEqualTo(128);
        assertThat(response.inventory()).isEqualTo(14);
        assertThat(response.seller()).isEqualTo(
                new SellerSummaryResponse(sellerId, "Harbor & Loom", new BigDecimal("4.90"))
        );
        assertThat(response.tags()).containsExactly("linen", "giftable");
        assertThat(response.featured()).isTrue();
    }

    @Test
    void exposesAnEmptyDescriptionWhenALegacyRowHasNullDescription() {
        Product product = new Product("Legacy item", null, new BigDecimal("12.00"), "USD");

        assertThat(ProductResponse.from(product).description()).isEmpty();
    }
}
