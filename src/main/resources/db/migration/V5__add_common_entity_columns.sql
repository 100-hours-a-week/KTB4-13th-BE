ALTER TABLE book_category
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' AFTER path;

ALTER TABLE carts
    ADD COLUMN deleted_at DATETIME(6) AFTER updated_at;

ALTER TABLE cart_item
    ADD COLUMN deleted_at DATETIME(6) AFTER updated_at;

ALTER TABLE addresses
    ADD COLUMN deleted_at DATETIME(6) AFTER updated_at;
