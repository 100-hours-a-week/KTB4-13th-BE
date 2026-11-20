ALTER TABLE product_popularity_snapshots
    ADD INDEX idx_product_popularity_snapshots_metrics_order (sales_quantity DESC, review_count DESC, review_rate DESC, product_id DESC);
