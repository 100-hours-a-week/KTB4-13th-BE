CREATE TABLE pg_operation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    input_hash VARCHAR(64) NOT NULL,
    pg_idempotency_key VARCHAR(36) NOT NULL,
    type VARCHAR(32) NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    completed BIT NOT NULL,
    result_id BIGINT NULL,
    rejected BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT udx_pg_operation_request UNIQUE (request_hash),
    CONSTRAINT udx_pg_operation_key UNIQUE (pg_idempotency_key),
    INDEX idx_pg_operation_pending (order_key, completed, rejected)
);
