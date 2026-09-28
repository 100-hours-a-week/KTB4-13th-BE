CREATE TABLE order_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    postal_code VARCHAR(5) NOT NULL,
    address VARCHAR(255) NOT NULL,
    detail_address VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    `key` VARCHAR(255),
    total_price DECIMAL(19, 2) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED',
    paid_at DATETIME(6),
    canceled_at DATETIME(6),
    address_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_key UNIQUE (`key`),
    CONSTRAINT fk_orders_address FOREIGN KEY (address_id) REFERENCES order_addresses (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    total_price DECIMAL(19, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
