CREATE TABLE product_restocks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    received_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    pending_product_id BIGINT GENERATED ALWAYS AS (IF(status = 'PENDING', product_id, NULL)) STORED,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_restocks_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uk_product_restocks_pending UNIQUE (pending_product_id),
    CONSTRAINT chk_product_restocks_quantity CHECK (quantity > 0),
    CONSTRAINT chk_product_restocks_status CHECK (status IN ('PENDING', 'COMPLETED')),
    INDEX idx_product_restocks_status_created_at (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
