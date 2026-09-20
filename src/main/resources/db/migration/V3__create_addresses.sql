CREATE TABLE addresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    label VARCHAR(255) NOT NULL,
    postal_code VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    detail_address VARCHAR(255),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_addresses_label_not_blank CHECK (CHAR_LENGTH(TRIM(label)) >= 1),
    CONSTRAINT ck_addresses_postal_code_not_blank CHECK (CHAR_LENGTH(TRIM(postal_code)) >= 1),
    CONSTRAINT ck_addresses_address_not_blank CHECK (CHAR_LENGTH(TRIM(address)) >= 1),
    INDEX idx_addresses_user_status_default (user_id, status, is_default)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
