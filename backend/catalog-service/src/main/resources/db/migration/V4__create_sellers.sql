-- Seller summaries are catalog-owned reference data, not identity accounts.
-- Preserve every existing product's seller instead of assigning legacy data
-- to one of the demo sellers. A fresh catalog has exactly three sellers.
CREATE TABLE sellers (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    rating NUMERIC(3, 2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sellers_rating_range CHECK (rating BETWEEN 0.00 AND 5.00)
);

INSERT INTO sellers (id, name, rating)
SELECT DISTINCT ON (seller_id) seller_id, seller_name, seller_rating
FROM products
ORDER BY seller_id, id;

ALTER TABLE products
    ALTER COLUMN seller_id SET DEFAULT '10000000-0000-4000-8000-000000000001'::uuid,
    ALTER COLUMN seller_name SET DEFAULT 'Harbor & Loom',
    ALTER COLUMN seller_rating SET DEFAULT 4.90,
    ADD CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES sellers (id);
