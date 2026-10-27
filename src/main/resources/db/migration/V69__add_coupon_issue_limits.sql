-- Legacy coupons had no total limit; preserve that behavior within the Java int range.
ALTER TABLE coupon
    ADD COLUMN total_quantity INT NOT NULL DEFAULT 2147483647,
    ADD COLUMN issued_quantity INT NOT NULL DEFAULT 0,
    ADD COLUMN starts_at DATETIME(6) NULL;

-- Soft-deleted ownership rows still reserve the unique user/coupon pair and count as prior issuance.
UPDATE coupon AS c
LEFT JOIN (
    SELECT coupon_id, COUNT(*) AS issued_quantity
    FROM owned_coupon
    GROUP BY coupon_id
) AS issued ON issued.coupon_id = c.id
SET c.issued_quantity = COALESCE(issued.issued_quantity, 0),
    c.starts_at = c.created_at;

ALTER TABLE coupon
    MODIFY COLUMN starts_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
