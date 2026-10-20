CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_notifications_user_positive CHECK (user_id > 0),
    CONSTRAINT ck_notifications_title_not_blank CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CONSTRAINT ck_notifications_body_not_blank CHECK (CHAR_LENGTH(TRIM(body)) > 0),
    INDEX idx_notifications_user_active_id (user_id, deleted_at, id),
    INDEX idx_notifications_user_active_read (user_id, deleted_at, read_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
