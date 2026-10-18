CREATE TABLE transaction_history
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    type VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    external_payment_key VARCHAR(255) NOT NULL,
    paid_amount DECIMAL(38,2) NOT NULL,
    point_amount DECIMAL(38,2) NOT NULL,
    coupon_amount DECIMAL(38,2) NOT NULL,
    message VARCHAR(255) NOT NULL,
    occurred_at DATETIME(6) NOT NULL
);
