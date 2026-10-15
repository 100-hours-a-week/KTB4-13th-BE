CREATE TABLE `cancel`
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    origin_amount DECIMAL(38,2) NOT NULL,
    owned_coupon_id BIGINT NOT NULL,
    coupon_discount DECIMAL(38,2) NOT NULL,
    used_point DECIMAL(38,2) NOT NULL,
    paid_amount DECIMAL(38,2) NOT NULL,
    canceled_quantity BIGINT NOT NULL,
    canceled_paid_amount DECIMAL(38,2) NOT NULL,
    canceled_point_amount DECIMAL(38,2) NOT NULL,
    canceled_coupon_amount DECIMAL(38,2) NOT NULL,
    external_cancel_key VARCHAR(255) NOT NULL,
    canceled_at DATETIME(6) NOT NULL
);
