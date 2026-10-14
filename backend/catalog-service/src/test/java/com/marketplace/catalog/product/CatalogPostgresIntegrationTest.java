package com.marketplace.catalog.product;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CatalogPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywaySeedsTwelveCompleteProductsAcrossThreeSellers() {
        List<Product> products = productRepository.findByActiveTrueOrderByNameAsc();

        assertThat(products).hasSize(12);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sellers", Integer.class)).isEqualTo(3);
        assertThat(products).extracting(Product::getName).isSorted();
        assertThat(products).extracting(Product::getId)
                .containsExactlyInAnyOrder(
                        UUID.fromString("20000000-0000-4000-8000-000000000001"),
                        UUID.fromString("20000000-0000-4000-8000-000000000002"),
                        UUID.fromString("20000000-0000-4000-8000-000000000003"),
                        UUID.fromString("20000000-0000-4000-8000-000000000004"),
                        UUID.fromString("20000000-0000-4000-8000-000000000005"),
                        UUID.fromString("20000000-0000-4000-8000-000000000006"),
                        UUID.fromString("20000000-0000-4000-8000-000000000007"),
                        UUID.fromString("20000000-0000-4000-8000-000000000008"),
                        UUID.fromString("20000000-0000-4000-8000-000000000009"),
                        UUID.fromString("20000000-0000-4000-8000-000000000010"),
                        UUID.fromString("20000000-0000-4000-8000-000000000011"),
                        UUID.fromString("20000000-0000-4000-8000-000000000012")
                );

        Set<UUID> sellerIds = products.stream()
                .map(Product::getSellerId)
                .collect(Collectors.toSet());
        assertThat(sellerIds).containsExactlyInAnyOrder(
                UUID.fromString("10000000-0000-4000-8000-000000000001"),
                UUID.fromString("10000000-0000-4000-8000-000000000002"),
                UUID.fromString("10000000-0000-4000-8000-000000000003")
        );
        sellerIds.forEach(sellerId -> assertThat(products.stream()
                .filter(product -> sellerId.equals(product.getSellerId()))
                .count()).isEqualTo(4));

        assertThat(products).allSatisfy(product -> {
            assertThat(product.getCategory()).isNotBlank();
            assertThat(product.getPrice()).isGreaterThan(BigDecimal.ZERO);
            assertThat(product.getCurrency()).isEqualTo("USD");
            assertThat(product.getRating()).isBetween(BigDecimal.ZERO, new BigDecimal("5.00"));
            assertThat(product.getReviewCount()).isPositive();
            assertThat(product.getInventory()).isPositive();
            assertThat(product.getSellerName()).isNotBlank();
            assertThat(product.getSellerRating()).isBetween(BigDecimal.ZERO, new BigDecimal("5.00"));
            assertThat(product.getTags()).isNotEmpty();
        });
    }

    @Test
    void rerunningFlywayLeavesTheDeterministicSeedUnchanged() {
        flyway.migrate();

        assertThat(productRepository.count()).isEqualTo(12);
        assertThat(productRepository.findByActiveTrueOrderByNameAsc()).hasSize(12);
    }
}
