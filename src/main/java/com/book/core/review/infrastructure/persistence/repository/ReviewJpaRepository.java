package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.review.domain.Review;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReviewJpaRepository extends JpaRepository<Review, Long> {
    List<Review> findByUserIdAndReviewKeyIn(final Long userId, final List<String> reviewKeys);

    Optional<Review> findByIdAndDeletedAtIsNull(final Long id);

    boolean existsByUserIdAndOrderItemIdAndDeletedAtIsNull(final Long userId, final Long orderItemId);
}
