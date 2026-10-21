INSERT INTO carts (user_id, created_at, updated_at)
SELECT users.id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM users
WHERE users.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM carts
      WHERE carts.user_id = users.id
  );
