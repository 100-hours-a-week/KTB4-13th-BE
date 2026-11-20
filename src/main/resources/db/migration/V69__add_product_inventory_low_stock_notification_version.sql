ALTER TABLE product_inventory
    ADD COLUMN low_stock_notification_version BIGINT NOT NULL DEFAULT 0;
