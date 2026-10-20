ALTER TABLE product_inventory
    ADD COLUMN low_stock_cycle BIGINT NOT NULL DEFAULT 0;
