CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    slug VARCHAR(140) NOT NULL UNIQUE,
    parent_id BIGINT,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    slug VARCHAR(140) NOT NULL UNIQUE,
    logo_url VARCHAR(255),
    country VARCHAR(80)
);

ALTER TABLE products ADD COLUMN IF NOT EXISTS category_id BIGINT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS brand_id BIGINT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS slug VARCHAR(180);
ALTER TABLE products ADD COLUMN IF NOT EXISTS sku_prefix VARCHAR(40);
ALTER TABLE products ADD COLUMN IF NOT EXISTS base_price DOUBLE PRECISION;
ALTER TABLE products ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'smartphones') THEN
        INSERT INTO categories(name, slug, sort_order) VALUES ('Smartphones', 'smartphones', 10);
    END IF;
END $$;

INSERT INTO brands(name, slug)
SELECT DISTINCT
    CASE
        WHEN LOWER(COALESCE(name, '')) LIKE 'apple iphone%' OR LOWER(COALESCE(name, '')) LIKE 'iphone%' THEN 'Apple'
        WHEN LOWER(COALESCE(name, '')) LIKE 'samsung%' OR LOWER(COALESCE(name, '')) LIKE 'galaxy%' THEN 'Samsung'
        WHEN LOWER(COALESCE(name, '')) LIKE 'google%' OR LOWER(COALESCE(name, '')) LIKE 'pixel%' THEN 'Google'
        WHEN LOWER(COALESCE(name, '')) LIKE 'oppo%' OR LOWER(COALESCE(name, '')) LIKE 'find %' THEN 'OPPO'
        WHEN LOWER(COALESCE(name, '')) LIKE 'vivo%' THEN 'Vivo'
        WHEN LOWER(COALESCE(name, '')) LIKE 'xiaomi%' THEN 'Xiaomi'
        WHEN LOWER(COALESCE(name, '')) LIKE 'sony%' OR LOWER(COALESCE(name, '')) LIKE 'xperia%' THEN 'Sony'
        WHEN LOWER(COALESCE(name, '')) LIKE 'asus%' OR LOWER(COALESCE(name, '')) LIKE 'rog%' THEN 'ASUS'
        WHEN LOWER(COALESCE(name, '')) LIKE 'zte%' OR LOWER(COALESCE(name, '')) LIKE 'nubia%' OR LOWER(COALESCE(name, '')) LIKE 'redmagic%' THEN 'ZTE'
        WHEN LOWER(COALESCE(name, '')) LIKE 'huawei%' THEN 'Huawei'
        WHEN LOWER(COALESCE(name, '')) LIKE 'honor%' THEN 'Honor'
        ELSE 'Other'
    END AS brand_name,
    CASE
        WHEN LOWER(COALESCE(name, '')) LIKE 'apple iphone%' OR LOWER(COALESCE(name, '')) LIKE 'iphone%' THEN 'apple'
        WHEN LOWER(COALESCE(name, '')) LIKE 'samsung%' OR LOWER(COALESCE(name, '')) LIKE 'galaxy%' THEN 'samsung'
        WHEN LOWER(COALESCE(name, '')) LIKE 'google%' OR LOWER(COALESCE(name, '')) LIKE 'pixel%' THEN 'google'
        WHEN LOWER(COALESCE(name, '')) LIKE 'oppo%' OR LOWER(COALESCE(name, '')) LIKE 'find %' THEN 'oppo'
        WHEN LOWER(COALESCE(name, '')) LIKE 'vivo%' THEN 'vivo'
        WHEN LOWER(COALESCE(name, '')) LIKE 'xiaomi%' THEN 'xiaomi'
        WHEN LOWER(COALESCE(name, '')) LIKE 'sony%' OR LOWER(COALESCE(name, '')) LIKE 'xperia%' THEN 'sony'
        WHEN LOWER(COALESCE(name, '')) LIKE 'asus%' OR LOWER(COALESCE(name, '')) LIKE 'rog%' THEN 'asus'
        WHEN LOWER(COALESCE(name, '')) LIKE 'zte%' OR LOWER(COALESCE(name, '')) LIKE 'nubia%' OR LOWER(COALESCE(name, '')) LIKE 'redmagic%' THEN 'zte'
        WHEN LOWER(COALESCE(name, '')) LIKE 'huawei%' THEN 'huawei'
        WHEN LOWER(COALESCE(name, '')) LIKE 'honor%' THEN 'honor'
        ELSE 'other'
    END AS brand_slug
FROM products
WHERE name IS NOT NULL
ON CONFLICT (slug) DO NOTHING;

UPDATE products p
SET category_id = c.id
FROM categories c
WHERE c.slug = 'smartphones' AND p.category_id IS NULL;

UPDATE products p
SET brand_id = b.id
FROM brands b
WHERE p.brand_id IS NULL
  AND b.slug = CASE
        WHEN LOWER(COALESCE(p.name, '')) LIKE 'apple iphone%' OR LOWER(COALESCE(p.name, '')) LIKE 'iphone%' THEN 'apple'
        WHEN LOWER(COALESCE(p.name, '')) LIKE 'samsung%' OR LOWER(COALESCE(p.name, '')) LIKE 'galaxy%' THEN 'samsung'
        WHEN LOWER(COALESCE(p.name, '')) LIKE 'google%' OR LOWER(COALESCE(p.name, '')) LIKE 'pixel%' THEN 'google'
        WHEN LOWER(COALESCE(p.name, '')) LIKE 'oppo%' OR LOWER(COALESCE(p.name, '')) LIKE 'find %' THEN 'oppo'
        WHEN LOWER(COALESCE(p.name, '')) LIKE 'vivo%' THEN 'vivo'
        WHEN LOWER(COALESCE(p.name, '')) LIKE 'xiaomi%' THEN 'xiaomi'
        WHEN LOWER(COALESCE(p.name, '')) LIKE 'sony%' OR LOWER(COALESCE(p.name, '')) LIKE 'xperia%' THEN 'sony'
        WHEN LOWER(COALESCE(p.name, '')) LIKE 'asus%' OR LOWER(COALESCE(p.name, '')) LIKE 'rog%' THEN 'asus'
        WHEN LOWER(COALESCE(p.name, '')) LIKE 'zte%' OR LOWER(COALESCE(p.name, '')) LIKE 'nubia%' OR LOWER(COALESCE(p.name, '')) LIKE 'redmagic%' THEN 'zte'
        WHEN LOWER(COALESCE(p.name, '')) LIKE 'huawei%' THEN 'huawei'
        WHEN LOWER(COALESCE(p.name, '')) LIKE 'honor%' THEN 'honor'
        ELSE 'other'
  END;

UPDATE products
SET slug = COALESCE(slug,
    LOWER(REGEXP_REPLACE(REGEXP_REPLACE(COALESCE(name, ''), '[^a-zA-Z0-9]+', '-', 'g'), '(^-|-$)', '', 'g')) || '-' || id
)
WHERE slug IS NULL OR slug = '';

UPDATE products
SET sku_prefix = COALESCE(sku_prefix,
    UPPER(SUBSTRING(REGEXP_REPLACE(COALESCE(name, ''), '[^A-Za-z0-9]+', '', 'g') FROM 1 FOR 6))
)
WHERE sku_prefix IS NULL OR sku_prefix = '';

UPDATE products
SET sku_prefix = 'SKU' || id
WHERE sku_prefix IS NULL OR sku_prefix = '';

UPDATE products
SET base_price = COALESCE(base_price, price, 0.0),
    price = COALESCE(price, base_price, 0.0),
    stock = COALESCE(stock, 0);

CREATE TABLE IF NOT EXISTS product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(80) NOT NULL UNIQUE,
    color VARCHAR(80),
    storage VARCHAR(80),
    ram VARCHAR(80),
    price_override DOUBLE PRECISION,
    stock INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_product_variants_product ON product_variants(product_id);
CREATE INDEX IF NOT EXISTS idx_product_variants_active ON product_variants(active);

CREATE TABLE IF NOT EXISTS product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_product_images_product ON product_images(product_id);

CREATE TABLE IF NOT EXISTS product_specs (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    spec_key VARCHAR(120) NOT NULL,
    spec_value VARCHAR(255),
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_product_specs_product ON product_specs(product_id);

INSERT INTO product_variants(product_id, sku, color, storage, ram, price_override, stock, active)
SELECT
    p.id,
    p.sku_prefix || '-' || p.id,
    'Default',
    NULLIF(TRIM(COALESCE(p.storage, '')), ''),
    NULLIF(TRIM(COALESCE(p.ram, '')), ''),
    NULL,
    COALESCE(p.stock, 0),
    TRUE
FROM products p
WHERE NOT EXISTS (
    SELECT 1 FROM product_variants pv WHERE pv.product_id = p.id
);

INSERT INTO product_images(product_id, url, sort_order, is_primary)
SELECT p.id, p.image_url, 0, TRUE
FROM products p
WHERE p.image_url IS NOT NULL
  AND p.image_url <> ''
  AND NOT EXISTS (
    SELECT 1 FROM product_images pi WHERE pi.product_id = p.id
  );

INSERT INTO product_specs(product_id, spec_key, spec_value, sort_order)
SELECT p.id, x.spec_key, x.spec_value, x.sort_order
FROM products p
CROSS JOIN LATERAL (
    VALUES
      ('OS', NULLIF(TRIM(COALESCE(p.os, '')), ''), 10),
      ('Chipset', NULLIF(TRIM(COALESCE(p.chipset, '')), ''), 20),
      ('CPU Speed', NULLIF(TRIM(COALESCE(p.speed, '')), ''), 30),
      ('RAM', NULLIF(TRIM(COALESCE(p.ram, '')), ''), 40),
      ('Storage', NULLIF(TRIM(COALESCE(p.storage, '')), ''), 50),
      ('Screen Size', NULLIF(TRIM(COALESCE(p.size, '')), ''), 60),
      ('Resolution', NULLIF(TRIM(COALESCE(p.resolution, '')), ''), 70),
      ('Battery', NULLIF(TRIM(COALESCE(p.battery, '')), ''), 80),
      ('Charging', NULLIF(TRIM(COALESCE(p.charging, '')), ''), 90)
) AS x(spec_key, spec_value, sort_order)
WHERE x.spec_value IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM product_specs ps
    WHERE ps.product_id = p.id
  );

ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS variant_id BIGINT;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS variant_id BIGINT;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS variant_sku VARCHAR(80);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS variant_label VARCHAR(180);

UPDATE cart_items ci
SET variant_id = pv.id
FROM product_variants pv
WHERE ci.product_id = pv.product_id
  AND ci.variant_id IS NULL
  AND pv.active = TRUE;

UPDATE order_items oi
SET variant_id = pv.id,
    variant_sku = pv.sku,
    variant_label = TRIM(BOTH ' /' FROM CONCAT(COALESCE(NULLIF(pv.color, ''), ''),
                                                CASE WHEN pv.color IS NOT NULL AND pv.color <> '' AND pv.storage IS NOT NULL AND pv.storage <> '' THEN ' / ' ELSE '' END,
                                                COALESCE(NULLIF(pv.storage, ''), '')))
FROM product_variants pv
WHERE oi.product_id = pv.product_id
  AND oi.variant_id IS NULL
  AND pv.active = TRUE;

ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS uk_cart_items_user_product;
CREATE UNIQUE INDEX IF NOT EXISTS uk_cart_items_user_variant ON cart_items(user_email, variant_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_variant_id ON cart_items(variant_id);
CREATE INDEX IF NOT EXISTS idx_order_items_variant_id ON order_items(variant_id);

ALTER TABLE products
    ADD CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id);
ALTER TABLE products
    ADD CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands(id);
