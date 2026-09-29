CREATE TABLE onboarding_book_candidates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    book_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_onboarding_book_candidates_book_id UNIQUE (book_id),
    CONSTRAINT fk_onboarding_book_candidates_book_id FOREIGN KEY (book_id) REFERENCES books (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
