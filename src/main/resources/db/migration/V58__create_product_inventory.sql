CREATE TABLE product_inventory (
    product_id BIGINT NOT NULL,
    on_hand_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    reorder_point INT NOT NULL DEFAULT 3,
    target_stock INT NOT NULL DEFAULT 20,
    PRIMARY KEY (product_id),
    CONSTRAINT fk_product_inventory_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_product_inventory_quantity CHECK (reserved_quantity >= 0 AND reserved_quantity <= on_hand_quantity),
    CONSTRAINT chk_product_inventory_restock CHECK (reorder_point >= 0 AND target_stock > reorder_point)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO product_inventory (product_id, on_hand_quantity, reserved_quantity, reorder_point, target_stock)
SELECT id, GREATEST(stock_quantity, 0), 0, 3, 20 FROM products;
