ALTER TABLE users
    ADD COLUMN delete_requested_at DATETIME(6) NULL AFTER deleted_at;

ALTER TABLE user_providers
    ADD COLUMN unlinked_at DATETIME(6) NULL AFTER deleted_at;
