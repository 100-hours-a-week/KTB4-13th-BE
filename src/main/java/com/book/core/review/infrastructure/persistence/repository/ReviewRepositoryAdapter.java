package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.common.paging.OffsetLimit;
import com.book.common.paging.Page;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.domain.ReviewTarget;
import java.util.Optional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepositoryPort {
    private final ReviewJpaRepository jpaRepository;
    private final ReviewQueryRepository queryRepository;

    @Override
    public Page<ReviewItemResult> findActiveReviews(final ReviewTarget target, final OffsetLimit offsetLimit) {
        return queryRepository.findActiveReviews(target, offsetLimit);
    }

    @Override
    public List<String> findUsedReviewKeys(final Long userId, final List<String> reviewKeys) {
        return jpaRepository.findByUserIdAndReviewKeyIn(userId, reviewKeys).stream().map(Review::reviewKey).toList();
    }

    @Override
    public List<Review> findByUserIdAndReviewKeyIn(final Long userId, final List<String> reviewKeys) {
        return jpaRepository.findByUserIdAndReviewKeyIn(userId, reviewKeys);
    }

    @Override
    public Review save(final Review review) {
        return jpaRepository.save(review);
    }

    @Override
    public Optional<Review> findActiveById(final Long reviewId) {
        return jpaRepository.findByIdAndDeletedAtIsNull(reviewId);
    }

    @Override
    public Optional<Review> findByIdIncludingDeleted(final Long reviewId) {
        return jpaRepository.findById(reviewId);
    }

    @Override
    public Optional<Review> findActiveByIdWithLock(final Long reviewId) {
        return queryRepository.findActiveByIdWithLock(reviewId);
    }

    @Override
    public boolean existsByUserIdAndReviewKey(final Long userId, final String reviewKey) {
        return jpaRepository.existsByUserIdAndReviewKey(userId, reviewKey);
    }

    @Override
    public Optional<Review> findByIdAndUserId(final Long reviewId, final Long userId) {
        return jpaRepository.findByIdAndUserIdAndDeletedAtIsNull(reviewId, userId);
    }
}
