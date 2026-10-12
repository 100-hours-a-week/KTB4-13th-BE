ALTER TABLE reviews
    DROP CHECK chk_reviews_rating,
    RENAME COLUMN rating TO rate;

ALTER TABLE reviews
    ADD CONSTRAINT chk_reviews_rate CHECK (rate BETWEEN 0.0 AND 10.0);
