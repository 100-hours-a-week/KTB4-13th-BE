ALTER TABLE orders ADD COLUMN expires_at DATETIME(6) NULL;

CREATE INDEX idx_orders_status_expires_at ON orders (status, expires_at);

UPDATE orders
SET status = 'EXPIRED'
WHERE status = 'CREATED'
  AND NOT EXISTS (
    SELECT 1 FROM pg_operation operation
    WHERE operation.order_key = orders.`key` AND operation.completed = 0 AND operation.rejected = 0
  );
