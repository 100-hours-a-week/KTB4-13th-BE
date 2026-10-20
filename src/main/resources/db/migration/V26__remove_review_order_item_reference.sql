ALTER TABLE reviews
    DROP FOREIGN KEY fk_reviews_order_item,
    DROP INDEX uk_reviews_user_order_active,
    DROP INDEX idx_reviews_order_item_id,
    DROP COLUMN order_item_id;
