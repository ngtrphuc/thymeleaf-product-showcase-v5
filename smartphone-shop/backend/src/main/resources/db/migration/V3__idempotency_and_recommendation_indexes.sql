CREATE TABLE order_idempotency_keys (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    order_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_idempotency_user_key UNIQUE (user_email, idempotency_key)
);

CREATE INDEX idx_order_idempotency_order_id ON order_idempotency_keys (order_id);
CREATE INDEX idx_order_items_product_order ON order_items (product_id, order_id);
