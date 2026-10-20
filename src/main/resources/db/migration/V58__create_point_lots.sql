CREATE TABLE point_lot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    point_history_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    original_amount DECIMAL(38, 2) NOT NULL,
    remaining_amount DECIMAL(38, 2) NOT NULL,
    expiry_notification_sent_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY udx_point_lot_history (point_history_id),
    KEY idx_point_lot_fifo (user_id, expires_at, remaining_amount),
    KEY idx_point_lot_expiry (expires_at, remaining_amount, expiry_notification_sent_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO point_lot (user_id, point_history_id, expires_at, original_amount, remaining_amount)
WITH earned AS (
    SELECT history.user_id,
           history.id,
           history.created_at,
           history.amount,
           COALESCE(SUM(history.amount) OVER (
               PARTITION BY history.user_id
               ORDER BY DATE_ADD(history.created_at, INTERVAL 1 YEAR), history.id
               ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
           ), 0) AS earned_before
    FROM point_history history
    WHERE history.deleted_at IS NULL
      AND history.amount > 0
), spent AS (
    SELECT history.user_id, SUM(-history.amount) AS total_spent
    FROM point_history history
    WHERE history.deleted_at IS NULL
      AND history.amount < 0
    GROUP BY history.user_id
)
SELECT earned.user_id,
       earned.id,
       DATE_ADD(earned.created_at, INTERVAL 1 YEAR),
       earned.amount,
       GREATEST(0, earned.amount - GREATEST(0, COALESCE(spent.total_spent, 0) - earned.earned_before))
FROM earned
LEFT JOIN spent ON spent.user_id = earned.user_id;
