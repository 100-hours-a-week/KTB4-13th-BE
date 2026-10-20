ALTER TABLE order_item
    ADD COLUMN confirmed_at DATETIME(6) NULL,
    ADD COLUMN confirmation_reminder_sent_at DATETIME(6) NULL,
    ADD COLUMN paid_at DATETIME(6) NULL;

UPDATE order_item item
JOIN orders order_header ON order_header.id = item.order_id
SET item.paid_at = order_header.paid_at
WHERE order_header.paid_at IS NOT NULL;
