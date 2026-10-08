ALTER TABLE product_popularity_snapshots
    DROP INDEX idx_product_popularity_snapshots_order,
    ADD INDEX idx_product_popularity_snapshots_order (sales_quantity DESC, product_id DESC);
