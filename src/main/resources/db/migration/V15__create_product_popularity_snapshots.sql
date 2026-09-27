CREATE TABLE product_popularity_snapshots (
    product_id BIGINT NOT NULL,
    sales_quantity BIGINT NOT NULL DEFAULT 0,
    review_count BIGINT NOT NULL DEFAULT 0,
    review_rate DECIMAL(7, 5) NOT NULL DEFAULT 0.00000,
    refreshed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (product_id),
    KEY idx_product_popularity_snapshots_order (
        sales_quantity DESC,
        review_count DESC,
        review_rate DESC,
        product_id DESC
    ),
    CONSTRAINT fk_product_popularity_snapshots_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
