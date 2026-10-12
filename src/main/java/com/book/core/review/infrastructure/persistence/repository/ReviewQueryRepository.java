package com.book.core.review.infrastructure.persistence.repository;

import static com.book.core.review.domain.QReview.review;

import com.book.common.paging.OffsetLimit;
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

    Page<ReviewItemResult> findActiveReviews(final ReviewTarget target, final OffsetLimit offsetLimit) {
        final var found = queryFactory
            .select(Projections.constructor(ReviewItemResult.class, review.id,
                Projections.constructor(ReviewTarget.class, review.targetType, review.targetId), review.rate, review.content))
            .from(review).where(review.targetType.eq(target.type()), review.targetId.eq(target.id()), review.deletedAt.isNull())
            .orderBy(review.id.desc()).offset(offsetLimit.offset()).limit((long) offsetLimit.limit() + 1).fetch();
        final boolean hasNext = found.size() > offsetLimit.limit();
        return new Page<>(found.subList(0, Math.min(found.size(), offsetLimit.limit())), hasNext);
    }
}
