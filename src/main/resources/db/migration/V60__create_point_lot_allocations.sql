CREATE TABLE point_lot_allocation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    debit_history_id BIGINT NOT NULL,
    point_lot_id BIGINT NOT NULL,
    order_item_id BIGINT NULL,
    amount DECIMAL(38, 2) NOT NULL,
    restored_amount DECIMAL(38, 2) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY udx_point_lot_allocation_debit_lot_item (debit_history_id, point_lot_id, order_item_id),
    KEY idx_point_lot_allocation_history (debit_history_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO point_lot_allocation (debit_history_id, point_lot_id, amount)
WITH history_sequence AS (
    SELECT history.user_id,
           history.id,
           history.type,
           history.amount,
           SUM(GREATEST(history.amount, 0)) OVER (
               PARTITION BY history.user_id ORDER BY history.created_at, history.id
               ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
           ) AS earned_before,
           SUM(GREATEST(history.amount, 0)) OVER (
               PARTITION BY history.user_id ORDER BY history.created_at, history.id
               ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
           ) AS earned_after,
           SUM(GREATEST(-history.amount, 0)) OVER (
               PARTITION BY history.user_id ORDER BY history.created_at, history.id
               ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
           ) AS spent_before,
           SUM(GREATEST(-history.amount, 0)) OVER (
               PARTITION BY history.user_id ORDER BY history.created_at, history.id
               ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
           ) AS spent_after
    FROM point_history history
    WHERE history.deleted_at IS NULL
      AND history.amount IS NOT NULL
), allocations AS (
    SELECT debit.id AS debit_history_id,
           lot.id AS point_lot_id,
           LEAST(earned.earned_after, debit.spent_after)
             - GREATEST(COALESCE(earned.earned_before, 0), COALESCE(debit.spent_before, 0)) AS amount
    FROM history_sequence debit
    JOIN history_sequence earned
      ON earned.user_id = debit.user_id
     AND earned.amount > 0
     AND (earned.created_at < debit.created_at
          OR (earned.created_at = debit.created_at AND earned.id < debit.id))
     AND LEAST(earned.earned_after, debit.spent_after)
         > GREATEST(COALESCE(earned.earned_before, 0), COALESCE(debit.spent_before, 0))
    JOIN point_lot lot ON lot.point_history_id = earned.id
    WHERE debit.amount < 0
      AND debit.type = 1
)
SELECT debit_history_id, point_lot_id, amount
FROM allocations
WHERE amount > 0;
