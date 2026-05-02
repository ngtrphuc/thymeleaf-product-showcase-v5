ALTER TABLE orders
    ADD COLUMN tracking_number VARCHAR(100),
    ADD COLUMN tracking_carrier VARCHAR(50),
    ADD COLUMN shipped_at TIMESTAMP,
    ADD COLUMN delivered_at TIMESTAMP,
    ADD COLUMN completed_at TIMESTAMP;

CREATE TABLE order_returns (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    refund_amount DOUBLE PRECISION,
    admin_note VARCHAR(500),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    CONSTRAINT uq_order_returns_order UNIQUE (order_id)
);

CREATE INDEX idx_order_returns_order ON order_returns (order_id);
CREATE INDEX idx_order_returns_status ON order_returns (status);
