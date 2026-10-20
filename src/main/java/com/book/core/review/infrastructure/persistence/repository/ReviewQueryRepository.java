package com.book.core.review.infrastructure.persistence.repository;

import static com.book.core.review.domain.QReview.review;

import com.book.common.paging.OffsetLimit;
import com.book.core.review.domain.Review;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import com.book.common.paging.Page;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.domain.ReviewTarget;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@RequiredArgsConstructor
class ReviewQueryRepository {
    private final JPAQueryFactory queryFactory;

    Optional<Review> findActiveByIdWithLock(final Long reviewId) {
        final JPAQuery<Review> reviewQuery = queryFactory.selectFrom(review).where(review.id.eq(reviewId).and(review.deletedAt.isNull()))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(reviewQuery.fetchOne());
    }

    Page<ReviewItemResult> findActiveReviews(final ReviewTarget target, final OffsetLimit offsetLimit) {
        final List<ReviewItemResult> found = queryFactory
            .select(Projections.constructor(ReviewItemResult.class, review.id,
                Projections.constructor(ReviewTarget.class, review.targetType, review.targetId), review.rate, review.content,
                review.isSpoiler, review.userId))
            .from(review).where(review.targetType.eq(target.type()), review.targetId.eq(target.id()), review.deletedAt.isNull())
            .orderBy(review.id.desc()).offset(offsetLimit.offset()).limit((long) offsetLimit.limit() + 1).fetch();
        final boolean hasNext = found.size() > offsetLimit.limit();
        return new Page<>(found.subList(0, Math.min(found.size(), offsetLimit.limit())), hasNext);
    }
}
