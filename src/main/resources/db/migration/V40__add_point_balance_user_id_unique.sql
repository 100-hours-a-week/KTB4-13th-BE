ALTER TABLE point_balance
    MODIFY user_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_point_balance_user_id UNIQUE (user_id);
