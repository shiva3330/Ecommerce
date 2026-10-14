package com.marketplace.catalog.product;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    private static final UUID DEFAULT_SELLER_ID =
            UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final String DEFAULT_SELLER_NAME = "Harbor & Loom";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 2_000)
    private String description;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    /** Demo availability only; inventory-service is the future source of truth. */
    @Column(nullable = false)
    private int inventory;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "seller_name", nullable = false, length = 160)
    private String sellerName;

    @Column(name = "seller_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal sellerRating;

    @Convert(converter = TagsJsonConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> tags = List.of();

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Product() {
        // Required by JPA.
    }

    public Product(String name, String description, BigDecimal price, String currency) {
        this(name, description, "Uncategorized", price, currency, BigDecimal.ZERO, 0, 0,
                DEFAULT_SELLER_ID, DEFAULT_SELLER_NAME, new BigDecimal("4.90"), List.of(), false);
    }

    public Product(
            String name,
            String description,
            String category,
            BigDecimal price,
            String currency,
            BigDecimal rating,
            int reviewCount,
            int inventory,
            UUID sellerId,
            String sellerName,
            BigDecimal sellerRating,
            List<String> tags,
            boolean featured
    ) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.currency = currency;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.inventory = inventory;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.sellerRating = sellerRating;
        this.tags = tags == null ? List.of() : List.copyOf(tags);
        this.featured = featured;
    }

    @PrePersist
    void assignCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public int getInventory() {
        return inventory;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public BigDecimal getSellerRating() {
        return sellerRating;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean isFeatured() {
        return featured;
    }
}
