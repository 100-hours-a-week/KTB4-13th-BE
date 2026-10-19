CREATE TABLE cart_proposals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    source_key VARCHAR(100) COLLATE utf8mb4_0900_bin NOT NULL,
    intent VARCHAR(2000) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    invalidated BOOLEAN NOT NULL DEFAULT FALSE,
    execution_id BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_proposal_source UNIQUE (user_id, source_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE cart_change_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    request_key VARCHAR(100) COLLATE utf8mb4_0900_bin NOT NULL,
    intent VARCHAR(2000) NOT NULL,
    proposal_id BIGINT,
    execution_id BIGINT,
    rejection VARCHAR(80),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_change_request_key UNIQUE (user_id, kind, request_key),
    CONSTRAINT fk_cart_request_proposal FOREIGN KEY (proposal_id) REFERENCES cart_proposals(id),
    CONSTRAINT chk_cart_request_outcome CHECK (execution_id IS NULL OR rejection IS NULL),
    CONSTRAINT chk_cart_request_kind CHECK (kind IN ('DIRECT', 'CONFIRMATION'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE cart_executions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    proposal_id BIGINT,
    applied_at TIMESTAMP(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_execution_request UNIQUE (request_id),
    CONSTRAINT uk_cart_execution_proposal UNIQUE (proposal_id),
    CONSTRAINT fk_cart_execution_request FOREIGN KEY (request_id) REFERENCES cart_change_requests(id),
    CONSTRAINT fk_cart_execution_proposal FOREIGN KEY (proposal_id) REFERENCES cart_proposals(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE cart_proposal_items (
    proposal_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    operation VARCHAR(10) NOT NULL,
    expected_quantity INT NOT NULL,
    quantity INT,
    expected_unit_price DECIMAL(19, 2),
    PRIMARY KEY (proposal_id, product_id),
    CONSTRAINT fk_cart_proposal_items_parent FOREIGN KEY (proposal_id) REFERENCES cart_proposals(id),
    CONSTRAINT chk_cart_proposal_items_expected CHECK (expected_quantity BETWEEN 0 AND 500),
    CONSTRAINT chk_cart_proposal_items_operation CHECK (
        (operation = 'REMOVE' AND expected_quantity > 0 AND quantity IS NULL AND expected_unit_price IS NULL)
        OR (operation IN ('ADD', 'UPDATE') AND quantity IS NOT NULL AND quantity BETWEEN 1 AND 500
            AND expected_unit_price IS NOT NULL AND expected_unit_price >= 0
            AND (operation = 'ADD' OR expected_quantity > 0))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE cart_change_request_items (
    request_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    operation VARCHAR(10) NOT NULL,
    expected_quantity INT NOT NULL,
    quantity INT,
    expected_unit_price DECIMAL(19, 2),
    PRIMARY KEY (request_id, product_id),
    CONSTRAINT fk_cart_change_request_items_parent FOREIGN KEY (request_id) REFERENCES cart_change_requests(id),
    CONSTRAINT chk_cart_change_request_items_expected CHECK (expected_quantity BETWEEN 0 AND 500),
    CONSTRAINT chk_cart_change_request_items_operation CHECK (
        (operation = 'REMOVE' AND expected_quantity > 0 AND quantity IS NULL AND expected_unit_price IS NULL)
        OR (operation IN ('ADD', 'UPDATE') AND quantity IS NOT NULL AND quantity BETWEEN 1 AND 500
            AND expected_unit_price IS NOT NULL AND expected_unit_price >= 0
            AND (operation = 'ADD' OR expected_quantity > 0))
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

ALTER TABLE cart_change_requests
    ADD CONSTRAINT fk_cart_request_execution FOREIGN KEY (execution_id) REFERENCES cart_executions(id);
ALTER TABLE cart_proposals
    ADD CONSTRAINT fk_cart_proposal_execution FOREIGN KEY (execution_id) REFERENCES cart_executions(id);
