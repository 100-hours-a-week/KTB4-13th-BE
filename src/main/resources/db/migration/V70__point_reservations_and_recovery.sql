ALTER TABLE point_balance
    MODIFY COLUMN balance DECIMAL(38, 2) NULL,
    ADD COLUMN reserved_amount DECIMAL(38, 2) NOT NULL DEFAULT 0;

ALTER TABLE point_history
    MODIFY COLUMN amount DECIMAL(38, 2) NULL,
    MODIFY COLUMN balance_after DECIMAL(38, 2) NULL;

CREATE TABLE point_unclassified_negative_balance (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    point_balance_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    original_balance DECIMAL(38, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_point_unclassified_balance UNIQUE (point_balance_id)
);

-- NOTE(케빈): 배포 전 기존 음수 잔액의 분류, 이력 대사, 이관 절차를 검토해 주세요.
INSERT INTO point_unclassified_negative_balance (point_balance_id, user_id, original_balance, created_at)
SELECT id, user_id, balance, CURRENT_TIMESTAMP(6) FROM point_balance WHERE balance < 0;
UPDATE point_balance SET balance = 0 WHERE balance < 0;

CREATE TABLE point_reservation (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(38, 2) NOT NULL,
    reservation_state VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT uk_point_reservation_attempt UNIQUE (attempt_id)
);

CREATE TABLE point_recovery (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    source_type VARCHAR(16) NOT NULL,
    source_reference_id BIGINT NOT NULL,
    original_amount DECIMAL(38, 2) NOT NULL,
    remaining_amount DECIMAL(38, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT uk_point_recovery_source UNIQUE (source_type, source_reference_id),
    INDEX idx_point_recovery_user_order (user_id, id)
);

CREATE TABLE point_recovery_settlement (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    recovery_id BIGINT NOT NULL,
    source_type VARCHAR(16) NOT NULL,
    source_reference_id BIGINT NOT NULL,
    amount DECIMAL(38, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL
);

CREATE TABLE point_earn_event (
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    source_type VARCHAR(16) NOT NULL,
    reference_id BIGINT NOT NULL,
    amount DECIMAL(38, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT uk_point_earn_event UNIQUE (user_id, source_type, reference_id)
);
