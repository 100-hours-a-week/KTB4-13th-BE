INSERT INTO point_balance (created_at, updated_at, user_id, balance, version)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), users.id, COALESCE(SUM(point_history.amount), 0), 0
FROM users
LEFT JOIN point_history ON point_history.user_id = users.id
WHERE users.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM point_balance
      WHERE point_balance.user_id = users.id
  )
GROUP BY users.id;
