-- Test-only assertion and cleanup for the V66 catalog fixture.
CREATE TEMPORARY TABLE onboarding_candidate_fixture_validation (
    is_valid TINYINT NOT NULL CHECK (is_valid = 1)
);
INSERT INTO onboarding_candidate_fixture_validation (is_valid)
SELECT IF(COUNT(*) = 490, 1, 0)
FROM onboarding_book_candidates candidate
JOIN books ON books.id = candidate.book_id
WHERE books.author = '__onboarding_candidate_flyway_test_fixture__';
INSERT INTO onboarding_candidate_fixture_validation (is_valid)
SELECT IF(COUNT(DISTINCT candidate.subcategory_code) = 49, 1, 0)
FROM onboarding_book_candidates candidate
JOIN books ON books.id = candidate.book_id
WHERE books.author = '__onboarding_candidate_flyway_test_fixture__';
INSERT INTO onboarding_candidate_fixture_validation (is_valid)
SELECT IF(
    COUNT(*) = 10
    AND MIN(candidate.display_order) = 1
    AND MAX(candidate.display_order) = 10
    AND COUNT(DISTINCT candidate.display_order) = 10,
    1,
    0
)
FROM onboarding_book_candidates candidate
JOIN books ON books.id = candidate.book_id
WHERE books.author = '__onboarding_candidate_flyway_test_fixture__'
GROUP BY candidate.subcategory_code;
DROP TEMPORARY TABLE onboarding_candidate_fixture_validation;

DELETE candidate
FROM onboarding_book_candidates candidate
JOIN books ON books.id = candidate.book_id
WHERE books.author = '__onboarding_candidate_flyway_test_fixture__';
DELETE FROM books WHERE author = '__onboarding_candidate_flyway_test_fixture__';
ALTER TABLE onboarding_book_candidates AUTO_INCREMENT = 1;
ALTER TABLE books AUTO_INCREMENT = 1;
