CREATE TABLE issuance_dlt_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    dlt_topic VARCHAR(249) NOT NULL,
    dlt_partition INT NOT NULL,
    dlt_offset BIGINT NOT NULL,
    message_key VARCHAR(255),
    payload MEDIUMTEXT,
    error_type VARCHAR(255) NOT NULL,
    received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    replay_requested_at DATETIME(6),
    replayed_at DATETIME(6),
    UNIQUE KEY uk_issuance_dlt_position (dlt_topic, dlt_partition, dlt_offset),
    KEY idx_issuance_dlt_replay (replayed_at, replay_requested_at, id)
);
