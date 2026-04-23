CREATE INDEX IF NOT EXISTS idx_order_idempotency_pending_created_at
    ON order_idempotency_keys (created_at)
    WHERE order_id IS NULL;
