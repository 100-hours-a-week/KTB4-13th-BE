CREATE TABLE recommendation_cards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    reason_long TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_recommendation_cards_user_id
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_recommendation_cards_book_id
        FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_recommendation_cards_user_id ON recommendation_cards (user_id);
