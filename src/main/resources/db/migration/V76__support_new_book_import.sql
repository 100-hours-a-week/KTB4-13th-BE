ALTER TABLE books
    ADD COLUMN normalized_isbn13 VARCHAR(20)
        GENERATED ALWAYS AS (REPLACE(REPLACE(isbn13, '-', ''), ' ', '')) VIRTUAL,
    ADD INDEX idx_books_normalized_isbn13 (normalized_isbn13),
    ADD COLUMN imported_isbn13 VARCHAR(13) NULL,
    ADD CONSTRAINT uq_books_imported_isbn13 UNIQUE (imported_isbn13),
    ADD CONSTRAINT chk_books_imported_isbn13 CHECK (imported_isbn13 IS NULL OR (isbn13 IS NOT NULL AND imported_isbn13 = isbn13));

CREATE TABLE book_detail_call_budget (
    id TINYINT NOT NULL PRIMARY KEY,
    quota_day DATE NULL,
    calls INT NOT NULL DEFAULT 0,
    next_call_at DATETIME(6) NULL
);

INSERT INTO book_detail_call_budget (id) VALUES (1);
