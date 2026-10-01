-- Legacy candidates retain NULL and are excluded from subcategory-filtered reads.
ALTER TABLE onboarding_book_candidates
    ADD COLUMN subcategory_code VARCHAR(30) NULL AFTER book_id,
    ADD INDEX idx_onboarding_candidates_book (book_id),
    ADD CONSTRAINT uk_onboarding_candidates_subcategory_book UNIQUE (subcategory_code, book_id),
    ADD INDEX idx_onboarding_candidates_subcategory_order (subcategory_code, display_order, book_id);

ALTER TABLE onboarding_book_candidates
    DROP INDEX uk_onboarding_book_candidates_book_id;
