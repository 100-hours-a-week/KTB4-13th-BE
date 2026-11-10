ALTER TABLE profile_sync_jobs
    ADD COLUMN kafka_publish_at DATETIME(6) NULL,
    ADD CONSTRAINT uk_profile_sync_jobs_revision UNIQUE (revision),
    ADD INDEX idx_profile_sync_jobs_kafka_publish (kafka_publish_at, user_id);
