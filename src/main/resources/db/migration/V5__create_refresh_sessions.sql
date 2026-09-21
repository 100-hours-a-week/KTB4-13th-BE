CREATE TABLE refresh_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    revoked_at DATETIME(6) NULL,
    active_flag TINYINT GENERATED ALWAYS AS (
        CASE WHEN revoked_at IS NULL THEN 1 ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_sessions_user_active_flag UNIQUE (user_id, active_flag),
    CONSTRAINT fk_refresh_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_refresh_sessions_token_hash (token_hash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
