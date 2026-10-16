CREATE TABLE settlement
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    merchant_id BIGINT NOT NULL,
    settlement_date DATE NOT NULL,
    original_amount DECIMAL(38,2) NOT NULL,
    fee_amount DECIMAL(38,2) NOT NULL,
    fee_rate DECIMAL(38,2) NOT NULL,
    settlement_amount DECIMAL(38,2) NOT NULL,
    state VARCHAR(32) NOT NULL,
    CONSTRAINT udx_settlement_merchant UNIQUE (settlement_date, merchant_id)
);

CREATE TABLE settlement_target
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    merchant_id BIGINT NOT NULL,
    settlement_date DATE NOT NULL,
    target_amount DECIMAL(38,2) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    transaction_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    unit_price DECIMAL(38,2) NOT NULL,
    total_price DECIMAL(38,2) NOT NULL
);
