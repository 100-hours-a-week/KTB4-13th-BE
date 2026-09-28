package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepositoryPort {
    private final ReviewJpaRepository jpaRepository;

    @Override
    public Review save(final Review review) {
        return jpaRepository.save(review);
    }

    @Override
    public Optional<Review> findActiveById(final Long reviewId) {
        return jpaRepository.findByIdAndDeletedAtIsNull(reviewId);
    }

    @Override
    public boolean existsActiveByUserIdAndOrderItemId(final Long userId, final Long orderItemId) {
        return jpaRepository.existsByUserIdAndOrderItemIdAndDeletedAtIsNull(userId, orderItemId);
    }
}
