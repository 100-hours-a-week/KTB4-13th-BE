ALTER TABLE pg_operation
    ADD COLUMN external_payment_key VARCHAR(200) NULL,
    ADD COLUMN approval_amount DECIMAL(19, 2) NULL,
    ADD COLUMN pg_order_id VARCHAR(64) NULL,
    ADD COLUMN attempt_id BIGINT NULL,
    ADD COLUMN uncertain_at DATETIME(6) NULL,
    ADD COLUMN review_required BIT NOT NULL DEFAULT FALSE,
    ADD COLUMN blocks_order BIT NOT NULL DEFAULT TRUE,
    ADD COLUMN compensation_requested BIT NOT NULL DEFAULT FALSE,
    ADD COLUMN compensation_idempotency_key VARCHAR(36) NULL,
    ADD COLUMN compensation_requested_at DATETIME(6) NULL,
    ADD INDEX idx_pg_operation_recovery (type, completed, rejected, review_required, requested_at);
