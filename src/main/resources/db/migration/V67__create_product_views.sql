CREATE TABLE product_views (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    viewed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_product_views_user_id CHECK (user_id > 0),
    CONSTRAINT chk_product_views_product_id CHECK (product_id > 0),
    INDEX idx_product_views_product_viewed_user (product_id, viewed_at, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
