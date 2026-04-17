CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    default_address VARCHAR(200),
    phone_number VARCHAR(20)
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    price DOUBLE PRECISION,
    image_url VARCHAR(255),
    stock INTEGER,
    os VARCHAR(255),
    chipset VARCHAR(255),
    speed VARCHAR(255),
    ram VARCHAR(255),
    storage VARCHAR(255),
    size VARCHAR(255),
    resolution VARCHAR(255),
    battery VARCHAR(255),
    charging VARCHAR(255),
    description VARCHAR(1000)
);

CREATE TABLE cart_items (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(100) NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT uk_cart_items_user_product UNIQUE (user_email, product_id)
);

CREATE INDEX idx_cart_items_user_email ON cart_items (user_email);
CREATE INDEX idx_cart_items_product_id ON cart_items (product_id);

CREATE TABLE compare_items (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(100) NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_compare_items_user_product UNIQUE (user_email, product_id)
);

CREATE INDEX idx_compare_user_created ON compare_items (user_email, created_at);
CREATE INDEX idx_compare_product ON compare_items (product_id);

CREATE TABLE wishlist_items (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(100) NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_wishlist_items_user_product UNIQUE (user_email, product_id)
);

CREATE INDEX idx_wishlist_user_created ON wishlist_items (user_email, created_at);
CREATE INDEX idx_wishlist_product ON wishlist_items (product_id);

CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(100) NOT NULL,
    type VARCHAR(40) NOT NULL,
    detail VARCHAR(200),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_user_email ON payment_methods (user_email);
CREATE INDEX idx_payment_user_active ON payment_methods (user_email, active);
CREATE INDEX idx_payment_user_default ON payment_methods (user_email, is_default);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(100) NOT NULL,
    customer_name VARCHAR(120),
    phone_number VARCHAR(30),
    shipping_address VARCHAR(255),
    total_amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    payment_method VARCHAR(40) NOT NULL DEFAULT 'CASH_ON_DELIVERY',
    payment_detail VARCHAR(200),
    payment_plan VARCHAR(20) DEFAULT 'FULL_PAYMENT',
    installment_months INTEGER,
    installment_monthly_amount BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_user_created ON orders (user_email, created_at);
CREATE INDEX idx_orders_status_created ON orders (status, created_at);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT,
    product_name VARCHAR(255),
    price DOUBLE PRECISION,
    quantity INTEGER
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(100) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    sender_role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_by_admin BOOLEAN NOT NULL DEFAULT FALSE,
    read_by_user BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_chat_user_created ON chat_messages (user_email, created_at);
CREATE INDEX idx_chat_unread_admin ON chat_messages (read_by_admin, user_email);
CREATE INDEX idx_chat_unread_user ON chat_messages (read_by_user, user_email);
