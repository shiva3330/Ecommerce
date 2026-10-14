CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_orders_customer_idempotency_key UNIQUE (customer_id, idempotency_key),
    CONSTRAINT chk_orders_customer_id_non_blank CHECK (length(btrim(customer_id)) > 0),
    CONSTRAINT chk_orders_idempotency_key_non_blank CHECK (length(btrim(idempotency_key)) > 0),
    CONSTRAINT chk_orders_idempotency_key_printable CHECK (idempotency_key !~ '[[:cntrl:]]'),
    CONSTRAINT chk_orders_status CHECK (status <> '')
);

CREATE TABLE order_items (
    order_id UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    PRIMARY KEY (order_id, line_number),
    CONSTRAINT chk_order_items_line_number CHECK (line_number >= 0),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    topic VARCHAR(200) NOT NULL,
    event_key VARCHAR(200) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    available_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    claim_token UUID,
    claimed_until TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    last_error TEXT,
    CONSTRAINT chk_outbox_payload_is_json_object CHECK (jsonb_typeof(payload::jsonb) = 'object'),
    CONSTRAINT chk_outbox_attempts_non_negative CHECK (attempts >= 0)
);

CREATE INDEX idx_outbox_ready
    ON outbox_events (available_at, created_at)
    WHERE published_at IS NULL;
