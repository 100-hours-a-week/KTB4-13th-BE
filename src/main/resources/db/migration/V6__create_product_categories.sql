CREATE TABLE product_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_product_category_category_product_status UNIQUE (category_id, product_id, status),
    CONSTRAINT fk_product_category_category FOREIGN KEY (category_id) REFERENCES book_category (id),
    CONSTRAINT fk_product_category_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_product_category_category_status_product (category_id, status, product_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_products_status_created_id ON products (status, created_at, id);
