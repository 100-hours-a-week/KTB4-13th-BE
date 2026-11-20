ALTER TABLE notifications
    ADD COLUMN event_key VARCHAR(128) NULL,
    ADD UNIQUE KEY uk_notifications_event_key (event_key);
