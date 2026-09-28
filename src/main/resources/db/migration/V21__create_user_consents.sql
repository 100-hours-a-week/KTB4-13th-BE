CREATE TABLE user_consents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    consent_type VARCHAR(50) NOT NULL,
    policy_version VARCHAR(50) NOT NULL,
    agreed_at DATETIME(6) NOT NULL,
    withdrawn_at DATETIME(6) NULL,
    active_flag TINYINT GENERATED ALWAYS AS (
        CASE WHEN withdrawn_at IS NULL THEN 1 ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_consents_user_type_active_flag UNIQUE (user_id, consent_type, active_flag),
    CONSTRAINT fk_user_consents_user_id FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
