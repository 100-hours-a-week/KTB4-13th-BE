ALTER TABLE coupon ADD COLUMN reserved_quantity BIGINT NOT NULL DEFAULT 0;

CREATE TABLE coupon_issue_request (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    max_use_count BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    published_at DATETIME(6),
    completed_at DATETIME(6),
    failed_at DATETIME(6),
    failure_reason VARCHAR(255),
    failure_payload VARCHAR(255),
    replay_requested BOOLEAN NOT NULL DEFAULT FALSE,
    generation INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_coupon_issue_request_user (coupon_id, user_id),
    KEY idx_coupon_issue_pending (coupon_id, completed_at),
    KEY idx_coupon_issue_outbox (published_at, id),
    KEY idx_coupon_issue_replay (replay_requested, id)
);
