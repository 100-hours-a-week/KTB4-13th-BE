ALTER TABLE order_item
    ADD COLUMN owned_coupon_id BIGINT NOT NULL DEFAULT -1,
    ADD COLUMN coupon_type VARCHAR(32) NOT NULL DEFAULT 'NONE',
    ADD COLUMN coupon_discount_value DECIMAL(38, 2) NOT NULL DEFAULT 0,
    ADD COLUMN coupon_min_order_amount DECIMAL(38, 2) NOT NULL DEFAULT 0,
    ADD COLUMN coupon_max_discount_amount DECIMAL(38, 2) NULL,
    ADD COLUMN used_point_amount DECIMAL(38, 2) NOT NULL DEFAULT 0;

ALTER TABLE coupon
    ADD COLUMN max_discount_amount DECIMAL(38, 2) NULL;

ALTER TABLE point_history
    ADD COLUMN event_key VARCHAR(150) NULL,
    ADD UNIQUE KEY udx_point_history_user_event (user_id, event_key);

CREATE TABLE signup_coupon_quota (
    coupon_id BIGINT NOT NULL,
    issue_limit INTEGER NOT NULL,
    issued_count INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (coupon_id),
    CONSTRAINT fk_signup_coupon_quota_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE signup_coupon_receipt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    coupon_id BIGINT NOT NULL,
    provider_email VARCHAR(254) NOT NULL,
    user_id BIGINT NULL,
    issued_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT udx_signup_coupon_receipt_email UNIQUE (coupon_id, provider_email),
    KEY idx_signup_coupon_receipt_user (coupon_id, user_id),
    KEY idx_signup_coupon_receipt_expiry (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO coupon (id, name, type, discount, max_use_count, min_order_amount, max_discount_amount, expired_at)
VALUES (1, '신규 회원 3,000원 할인', 'FIXED_AMOUNT', 3000, 1, 10000, NULL, '2026-11-17 23:59:59');

INSERT INTO signup_coupon_quota (coupon_id, issue_limit, issued_count)
VALUES (1, 10, 0);

INSERT INTO coupon_target (coupon_id, target_type, target_id)
SELECT 1, 'PRODUCT_CATEGORY', id
FROM book_category
WHERE name = '소설'
  AND deleted_at IS NULL;
