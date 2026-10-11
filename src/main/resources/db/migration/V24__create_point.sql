CREATE TABLE point_balance
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    created_at datetime(6)           NOT NULL,
    updated_at datetime(6)           NOT NULL,
    deleted_at datetime(6)           NULL,
    user_id    BIGINT NULL,
    balance    DECIMAL NULL,
    CONSTRAINT pk_point_balance PRIMARY KEY (id)
);

CREATE TABLE point_history
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    created_at    datetime(6)           NOT NULL,
    updated_at    datetime(6)           NOT NULL,
    deleted_at    datetime(6)           NULL,
    user_id       BIGINT NULL,
    type          SMALLINT NULL,
    reference_id  BIGINT NULL,
    amount        DECIMAL NULL,
    balance_after DECIMAL NULL,
    CONSTRAINT pk_point_history PRIMARY KEY (id)
);

