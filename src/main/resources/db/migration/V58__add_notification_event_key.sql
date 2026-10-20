ALTER TABLE notifications
    ADD COLUMN event_key VARCHAR(150) NULL,
    ADD UNIQUE KEY udx_notifications_event_key (event_key);
