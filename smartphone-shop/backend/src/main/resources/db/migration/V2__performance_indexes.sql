CREATE INDEX IF NOT EXISTS idx_products_name_lower ON products (LOWER(name));

CREATE INDEX IF NOT EXISTS idx_orders_user_status_created
    ON orders (user_email, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_messages_user_admin_sender_created
    ON chat_messages (user_email, read_by_admin, sender_role, created_at DESC);
