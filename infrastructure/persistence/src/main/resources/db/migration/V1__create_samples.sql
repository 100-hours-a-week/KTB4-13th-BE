CREATE TABLE samples (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_samples_name_not_blank CHECK (CHAR_LENGTH(TRIM(name)) >= 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
