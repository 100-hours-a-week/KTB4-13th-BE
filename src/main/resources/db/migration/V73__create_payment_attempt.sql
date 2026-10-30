CREATE TABLE payment_attempt
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    payment_id BIGINT NOT NULL,
    order_key VARCHAR(255) NOT NULL,
    pg_order_id VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    input_hash VARCHAR(64) NOT NULL,
    state VARCHAR(16) NOT NULL,
    CONSTRAINT uk_payment_attempt_pg_order_id UNIQUE (pg_order_id),
    CONSTRAINT uk_payment_attempt_request_hash UNIQUE (request_hash),
    CONSTRAINT fk_payment_attempt_payment FOREIGN KEY (payment_id) REFERENCES payment(id),
    INDEX idx_payment_attempt_payment (payment_id, id)
);
