CREATE TABLE profile_sync_jobs (
    user_id BIGINT NOT NULL,
    revision CHAR(36) NOT NULL,
    available_at DATETIME(6) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    INDEX idx_profile_sync_jobs_available (available_at, user_id),
    CONSTRAINT fk_profile_sync_jobs_user FOREIGN KEY (user_id) REFERENCES users (id)
);
