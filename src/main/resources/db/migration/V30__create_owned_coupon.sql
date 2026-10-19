CREATE TABLE owned_coupon (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    state VARCHAR(32) NOT NULL,
    max_use_count BIGINT NOT NULL,
    used_count BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY udx_owned_coupon (user_id, coupon_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
