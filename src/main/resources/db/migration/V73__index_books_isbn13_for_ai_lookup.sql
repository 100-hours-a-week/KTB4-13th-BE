-- An existing ISBN-leading index (including a UNIQUE index) already serves both lookups.
SET @isbn_index_ddl = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = 'books' AND column_name = 'isbn13'
          AND seq_in_index = 1 AND sub_part IS NULL AND is_visible = 'YES'
    ),
    'SELECT 1',
    'CREATE INDEX idx_books_isbn13 ON books (isbn13)'
);
PREPARE isbn_index_statement FROM @isbn_index_ddl;
EXECUTE isbn_index_statement;
DEALLOCATE PREPARE isbn_index_statement;
