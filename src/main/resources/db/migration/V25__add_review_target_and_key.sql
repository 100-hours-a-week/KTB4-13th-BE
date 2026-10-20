ALTER TABLE reviews
    ADD COLUMN review_key VARCHAR(64) NULL,
    ADD COLUMN target_type VARCHAR(16) NULL,
    ADD COLUMN target_id BIGINT NULL;

UPDATE reviews review
    JOIN order_item item ON item.id = review.order_item_id
SET review.review_key = CONCAT('ORDER_ITEM_', review.order_item_id),
    review.target_type = 'PRODUCT',
    review.target_id = item.product_id;

ALTER TABLE reviews
    MODIFY COLUMN review_key VARCHAR(64) NOT NULL,
    MODIFY COLUMN target_type VARCHAR(16) NOT NULL,
    MODIFY COLUMN target_id BIGINT NOT NULL,
    ADD UNIQUE INDEX udx_user_review (user_id, review_key),
    ADD INDEX idx_reviews_target_active_id (target_type, target_id, deleted_at, id);
