ALTER TABLE order_item
    ADD COLUMN item_name VARCHAR(255),
    ADD COLUMN thumbnail_url VARCHAR(255),
    ADD COLUMN author VARCHAR(255),
    ADD COLUMN sale_price DECIMAL(19, 2);
