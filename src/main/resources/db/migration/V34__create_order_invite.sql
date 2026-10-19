CREATE TABLE order_invite
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    order_id BIGINT NOT NULL,
    invite_key VARCHAR(255) NOT NULL,
    CONSTRAINT udx_order_invite_key UNIQUE (invite_key),
    INDEX idx_order_invite_order_id (order_id)
);
