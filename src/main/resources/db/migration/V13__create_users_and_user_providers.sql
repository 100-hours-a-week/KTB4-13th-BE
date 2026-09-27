CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nickname VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    active_flag TINYINT GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN 1 ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_nickname_active_flag UNIQUE (nickname, active_flag)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_providers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(254) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    active_flag TINYINT GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN 1 ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_providers_identity_active_flag
        UNIQUE (provider_type, provider_user_id, active_flag),
    CONSTRAINT fk_user_providers_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
