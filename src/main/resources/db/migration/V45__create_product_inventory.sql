CREATE TABLE product_inventory (
    product_id BIGINT NOT NULL PRIMARY KEY,
    on_hand_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    reorder_point INT NOT NULL DEFAULT 3,
    target_stock INT NOT NULL DEFAULT 20,
    CONSTRAINT fk_product_inventory_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_product_inventory_nonnegative CHECK (on_hand_quantity >= 0 AND reserved_quantity >= 0),
    CONSTRAINT chk_product_inventory_reservation CHECK (reserved_quantity <= on_hand_quantity),
    CONSTRAINT chk_product_inventory_threshold CHECK (reorder_point >= 0 AND target_stock > reorder_point)
);

INSERT INTO product_inventory (product_id, on_hand_quantity, reserved_quantity, reorder_point, target_stock)
SELECT id, stock_quantity, 0, 3, 20 FROM products;
