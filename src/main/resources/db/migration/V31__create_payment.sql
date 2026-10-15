CREATE TABLE payment
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    origin_amount DECIMAL(38,2) NOT NULL,
    owned_coupon_id BIGINT NOT NULL,
    coupon_discount DECIMAL(38,2) NOT NULL,
    used_point DECIMAL(38,2) NOT NULL,
    payer_user_id BIGINT NOT NULL,
    paid_amount DECIMAL(38,2) NOT NULL,
    state VARCHAR(32) NOT NULL,
    external_payment_key VARCHAR(255) NULL,
    method VARCHAR(32) NULL,
    approve_code VARCHAR(255) NULL,
    paid_at DATETIME(6) NULL,
    CONSTRAINT udx_payment_order_id UNIQUE (order_id)
);
