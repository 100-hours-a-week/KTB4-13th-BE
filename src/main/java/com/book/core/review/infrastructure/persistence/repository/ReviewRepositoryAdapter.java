package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import java.util.Optional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepositoryPort {
    private final ReviewJpaRepository jpaRepository;


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
    public boolean existsByUserIdAndReviewKey(final Long userId, final String reviewKey) {
        return jpaRepository.existsByUserIdAndReviewKey(userId, reviewKey);
    }

    @Override
    public Optional<Review> findByIdAndUserId(final Long reviewId, final Long userId) {
        return jpaRepository.findByIdAndUserIdAndDeletedAtIsNull(reviewId, userId);
    }
}
