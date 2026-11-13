ALTER TABLE product_popularity_snapshots
    DROP INDEX idx_product_popularity_snapshots_order,
    ADD INDEX idx_product_popularity_snapshots_order (sales_quantity DESC, review_count DESC, review_rate DESC, product_id DESC);
