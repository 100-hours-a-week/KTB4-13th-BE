ALTER TABLE user_providers
    ADD COLUMN unlinked_at DATETIME(6) NULL AFTER deleted_at;
