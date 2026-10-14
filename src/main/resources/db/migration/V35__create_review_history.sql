CREATE TABLE review_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(16) NOT NULL,
    target_id BIGINT NOT NULL,
    operation VARCHAR(16) NOT NULL,
    content VARCHAR(255) NOT NULL,
    rate DECIMAL(3, 1) NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_review_history_review_order (review_id, changed_at, id),
    INDEX idx_review_history_user (user_id),
    CONSTRAINT chk_review_history_operation CHECK (operation IN ('CREATE', 'UPDATE', 'DELETE')),
    CONSTRAINT chk_review_history_rate CHECK (rate BETWEEN 0.5 AND 5.0 AND MOD(rate * 10, 5) = 0)
);
