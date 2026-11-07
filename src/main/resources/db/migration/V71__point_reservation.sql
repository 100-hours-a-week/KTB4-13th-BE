ALTER TABLE point_balance
    ADD COLUMN reserved_amount DECIMAL NOT NULL DEFAULT 0;

CREATE TABLE point_reservation
(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    amount DECIMAL NOT NULL,
    reservation_state VARCHAR(16) NOT NULL,
    CONSTRAINT uk_point_reservation_attempt UNIQUE (attempt_id)
);
