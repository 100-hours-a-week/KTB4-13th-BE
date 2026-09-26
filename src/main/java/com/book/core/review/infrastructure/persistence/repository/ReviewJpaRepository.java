package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.review.domain.Review;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReviewJpaRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByIdAndDeletedAtIsNull(final Long id);

    boolean existsByUserIdAndOrderItemIdAndDeletedAtIsNull(final Long userId, final Long orderItemId);
}
