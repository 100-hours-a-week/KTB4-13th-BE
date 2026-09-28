-- The former status column only represented the deletion lifecycle.
-- Existing deleted rows use updated_at as the closest available deletion timestamp.

ALTER TABLE carts
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER status;

UPDATE carts
SET deleted_at = updated_at
WHERE status = 'DELETED';

ALTER TABLE carts
    DROP COLUMN status;

ALTER TABLE cart_item
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER status;

UPDATE cart_item
SET deleted_at = updated_at
WHERE status = 'DELETED';

ALTER TABLE cart_item
    DROP INDEX idx_cart_item_cart_created,
    ADD INDEX idx_cart_item_cart_deleted_created (cart_id, deleted_at, created_at, id),
    DROP COLUMN status;

ALTER TABLE addresses
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER status;

UPDATE addresses
SET deleted_at = updated_at
WHERE status = 'DELETED';

ALTER TABLE addresses
    DROP INDEX idx_addresses_user_status_default,
    ADD INDEX idx_addresses_user_deleted_default (user_id, deleted_at, is_default),
    DROP COLUMN status;

ALTER TABLE book_category
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER status;

UPDATE book_category
SET deleted_at = updated_at
WHERE status = 'DELETED';

ALTER TABLE book_category
    DROP COLUMN status;

ALTER TABLE books
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER status;

UPDATE books
SET deleted_at = updated_at
WHERE status = 'DELETED';

ALTER TABLE books
    DROP COLUMN status;

ALTER TABLE products
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER status;

UPDATE products
SET deleted_at = updated_at
WHERE status = 'DELETED';

ALTER TABLE products
    DROP INDEX idx_products_status_created_id,
    ADD INDEX idx_products_deleted_created_id (deleted_at, created_at, id),
    DROP COLUMN status;

ALTER TABLE product_category
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER status;

UPDATE product_category
SET deleted_at = updated_at
WHERE status = 'DELETED';

ALTER TABLE product_category
    DROP INDEX uk_product_category_category_product_status,
    DROP INDEX idx_product_category_category_status_product,
    ADD COLUMN active_flag TINYINT GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN 1 ELSE NULL END
    ) STORED AFTER deleted_at,
    ADD CONSTRAINT uk_product_category_category_product_active
        UNIQUE (category_id, product_id, active_flag),
    ADD INDEX idx_product_category_category_deleted_product (category_id, deleted_at, product_id),
    DROP COLUMN status;
