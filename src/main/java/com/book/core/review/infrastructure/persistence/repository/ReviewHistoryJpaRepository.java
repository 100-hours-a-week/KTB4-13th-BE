package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.review.domain.ReviewHistory;
import org.springframework.data.repository.Repository;

interface ReviewHistoryJpaRepository extends Repository<ReviewHistory, Long> {
    ReviewHistory save(final ReviewHistory history);
}
