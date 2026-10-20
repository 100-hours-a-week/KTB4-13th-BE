ALTER TABLE point_history
    ADD COLUMN event_key VARCHAR(150) NULL,
    ADD UNIQUE KEY udx_point_history_user_event (user_id, event_key);
