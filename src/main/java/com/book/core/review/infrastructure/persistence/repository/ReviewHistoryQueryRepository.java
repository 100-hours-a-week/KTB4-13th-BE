package com.book.core.review.infrastructure.persistence.repository;

import static com.book.core.review.domain.QReviewHistory.reviewHistory;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ReviewHistoryQueryRepository {
    private final JPAQueryFactory queryFactory;

    Optional<LocalDateTime> findLatestChangedAt(final Long reviewId) {
        return Optional.ofNullable(
            queryFactory.select(reviewHistory.changedAt.max()).from(reviewHistory).where(reviewHistory.reviewId.eq(reviewId)).fetchOne());
    }
}
