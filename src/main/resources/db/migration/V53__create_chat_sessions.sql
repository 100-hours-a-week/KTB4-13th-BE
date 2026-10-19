CREATE TABLE chat_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    active_request_id BIGINT,
    next_sequence BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_chat_session_user (user_id, id),
    CONSTRAINT chk_chat_sequence CHECK (next_sequence > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE chat_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    request_key VARCHAR(100) COLLATE utf8mb4_0900_bin NOT NULL,
    fingerprint CHAR(64) NOT NULL,
    state VARCHAR(12) NOT NULL,
    generation BIGINT NOT NULL,
    lease_until TIMESTAMP(6) NOT NULL,
    failure_code VARCHAR(80),
    retry_after_seconds BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_request_key UNIQUE (session_id, request_key),
    CONSTRAINT fk_chat_request_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id),
    CONSTRAINT chk_chat_request_generation CHECK (generation > 0),
    CONSTRAINT chk_chat_request_state CHECK (
        (state IN ('RUNNING', 'COMPLETED') AND failure_code IS NULL)
        OR (state = 'FAILED' AND failure_code IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    message_sequence BIGINT NOT NULL,
    role VARCHAR(10) NOT NULL,
    text LONGTEXT NOT NULL,
    response_payload LONGTEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_message_sequence UNIQUE (session_id, message_sequence),
    CONSTRAINT uk_chat_message_request_role UNIQUE (request_id, role),
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id),
    CONSTRAINT fk_chat_message_request FOREIGN KEY (request_id) REFERENCES chat_requests(id),
    CONSTRAINT chk_chat_message_sequence CHECK (message_sequence > 0),
    CONSTRAINT chk_chat_message_payload CHECK (
        (role = 'user' AND response_payload IS NULL) OR (role = 'assistant' AND response_payload IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

ALTER TABLE chat_sessions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
