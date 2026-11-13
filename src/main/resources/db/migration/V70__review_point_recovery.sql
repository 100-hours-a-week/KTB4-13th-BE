CREATE TABLE point_recovery (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    source_type VARCHAR(16) NOT NULL,
    source_reference_id BIGINT NOT NULL,
    original_amount DECIMAL(10, 0) NOT NULL,
    remaining_amount DECIMAL(10, 0) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT uk_point_recovery_source UNIQUE (source_type, source_reference_id),
    INDEX idx_point_recovery_user_order (user_id, id)
);

CREATE TABLE point_recovery_settlement (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    recovery_id BIGINT NOT NULL,
    source_type VARCHAR(16) NULL,
    source_reference_id BIGINT NULL,
    amount DECIMAL(10, 0) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL
);
