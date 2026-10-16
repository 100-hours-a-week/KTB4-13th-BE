CREATE TABLE merchant
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    name VARCHAR(255) NOT NULL,
    settlement_cycle INT NOT NULL
);

CREATE TABLE merchant_product_mapping
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    product_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    CONSTRAINT udx_merchant_product UNIQUE (merchant_id, product_id)
);
