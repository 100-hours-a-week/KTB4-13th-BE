package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.review.domain.ReviewHistory;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.Optional;

interface ReviewHistoryJpaRepository extends Repository<ReviewHistory, Long> {
    @Query("select max(h.changedAt) from ReviewHistory h where h.reviewId = :reviewId")
    Optional<LocalDateTime> findLatestChangedAt(final Long reviewId);

    ReviewHistory save(final ReviewHistory history);
}
