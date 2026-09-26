CREATE TABLE reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    rating DECIMAL(3, 1) NOT NULL,
    content VARCHAR(255) NOT NULL,
    is_spoiler BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    active_flag TINYINT GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN 1 ELSE NULL END) STORED,
    PRIMARY KEY (id),
    CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_item (id),
    CONSTRAINT uk_reviews_user_order_active UNIQUE (user_id, order_item_id, active_flag),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 0.0 AND 10.0),
    KEY idx_reviews_order_item_id (order_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
