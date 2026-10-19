CREATE TABLE cancel_balance
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    order_id BIGINT NOT NULL,
    cancellable_paid_amount DECIMAL(38,2) NOT NULL,
    cancellable_point_amount DECIMAL(38,2) NOT NULL,
    cancellable_coupon_amount DECIMAL(38,2) NOT NULL,
    cancelled_paid_amount DECIMAL(38,2) NOT NULL,
    cancelled_point_amount DECIMAL(38,2) NOT NULL,
    cancelled_coupon_amount DECIMAL(38,2) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT udx_cancel_balance_order_id UNIQUE (order_id)
);
