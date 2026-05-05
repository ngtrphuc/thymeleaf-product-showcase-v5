CREATE INDEX IF NOT EXISTS idx_orders_user_status
    ON orders (user_email, status);

CREATE INDEX IF NOT EXISTS idx_orders_created_id
    ON orders (created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_cart_items_user_product_lookup
    ON cart_items (user_email, product_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_user_variant_lookup
    ON cart_items (user_email, variant_id);

CREATE INDEX IF NOT EXISTS idx_wishlist_items_user_product_lookup
    ON wishlist_items (user_email, product_id);

CREATE INDEX IF NOT EXISTS idx_compare_items_user_lookup
    ON compare_items (user_email);

CREATE INDEX IF NOT EXISTS idx_chat_messages_user_created_desc
    ON chat_messages (user_email, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_products_brand_id
    ON products (brand_id)
    WHERE brand_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_product_variants_product_active
    ON product_variants (product_id, active);
