ALTER TABLE users
    ALTER COLUMN role TYPE VARCHAR(30);

CREATE TABLE addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label VARCHAR(50),
    recipient_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    postal_code VARCHAR(10),
    prefecture VARCHAR(30),
    city VARCHAR(50),
    street_address VARCHAR(200) NOT NULL,
    building VARCHAR(100),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_addresses_user ON addresses (user_id);
CREATE INDEX idx_addresses_user_default ON addresses (user_id, is_default);

INSERT INTO addresses (user_id, recipient_name, street_address, is_default)
SELECT id, full_name, default_address, TRUE
FROM users
WHERE default_address IS NOT NULL
  AND btrim(default_address) <> '';
