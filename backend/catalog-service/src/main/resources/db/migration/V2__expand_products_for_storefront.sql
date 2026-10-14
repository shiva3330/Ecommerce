ALTER TABLE products
    ADD COLUMN category VARCHAR(80),
    ADD COLUMN rating NUMERIC(3, 2),
    ADD COLUMN review_count INTEGER,
    ADD COLUMN inventory INTEGER,
    ADD COLUMN seller_id UUID,
    ADD COLUMN seller_name VARCHAR(160),
    ADD COLUMN seller_rating NUMERIC(3, 2),
    ADD COLUMN tags TEXT,
    ADD COLUMN featured BOOLEAN;

-- Existing catalog rows predate the storefront shape. Give them valid response
-- values before making the new fields required for all future rows.
UPDATE products
SET category = COALESCE(category, 'Uncategorized'),
    rating = COALESCE(rating, 0.00),
    review_count = COALESCE(review_count, 0),
    inventory = COALESCE(inventory, 0),
    seller_id = COALESCE(seller_id, '00000000-0000-4000-8000-000000000001'::uuid),
    seller_name = COALESCE(seller_name, 'Unknown Seller'),
    seller_rating = COALESCE(seller_rating, 0.00),
    tags = COALESCE(tags, '[]'),
    featured = COALESCE(featured, FALSE);

ALTER TABLE products
    ALTER COLUMN category SET DEFAULT 'Uncategorized',
    ALTER COLUMN category SET NOT NULL,
    ALTER COLUMN rating SET DEFAULT 0.00,
    ALTER COLUMN rating SET NOT NULL,
    ALTER COLUMN review_count SET DEFAULT 0,
    ALTER COLUMN review_count SET NOT NULL,
    ALTER COLUMN inventory SET DEFAULT 0,
    ALTER COLUMN inventory SET NOT NULL,
    ALTER COLUMN seller_id SET DEFAULT '00000000-0000-4000-8000-000000000001'::uuid,
    ALTER COLUMN seller_id SET NOT NULL,
    ALTER COLUMN seller_name SET DEFAULT 'Unknown Seller',
    ALTER COLUMN seller_name SET NOT NULL,
    ALTER COLUMN seller_rating SET DEFAULT 0.00,
    ALTER COLUMN seller_rating SET NOT NULL,
    ALTER COLUMN tags SET DEFAULT '[]',
    ALTER COLUMN tags SET NOT NULL,
    ALTER COLUMN featured SET DEFAULT FALSE,
    ALTER COLUMN featured SET NOT NULL;

ALTER TABLE products
    ADD CONSTRAINT chk_products_rating_range CHECK (rating BETWEEN 0.00 AND 5.00),
    ADD CONSTRAINT chk_products_review_count_non_negative CHECK (review_count >= 0),
    ADD CONSTRAINT chk_products_inventory_non_negative CHECK (inventory >= 0),
    ADD CONSTRAINT chk_products_seller_rating_range CHECK (seller_rating BETWEEN 0.00 AND 5.00);

COMMENT ON COLUMN products.inventory IS
    'Demo availability only; inventory-service is the future source of truth.';
