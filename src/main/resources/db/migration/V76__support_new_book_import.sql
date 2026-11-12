ALTER TABLE books
    ADD COLUMN normalized_isbn13 VARCHAR(20)
        GENERATED ALWAYS AS (REPLACE(REPLACE(isbn13, '-', ''), ' ', '')) VIRTUAL,
    ADD INDEX idx_books_normalized_isbn13 (normalized_isbn13),
    ADD COLUMN imported_isbn13 VARCHAR(13) NULL,
    ADD CONSTRAINT uq_books_imported_isbn13 UNIQUE (imported_isbn13),
    ADD CONSTRAINT chk_books_imported_isbn13 CHECK (imported_isbn13 IS NULL OR (isbn13 IS NOT NULL AND imported_isbn13 = isbn13));
