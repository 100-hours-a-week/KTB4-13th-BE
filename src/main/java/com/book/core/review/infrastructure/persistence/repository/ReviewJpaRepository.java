package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.review.domain.Review;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface ReviewJpaRepository extends JpaRepository<Review, Long> {
    List<Review> findByUserIdAndReviewKeyIn(final Long userId, final List<String> reviewKeys);

    Optional<Review> findByIdAndDeletedAtIsNull(final Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Review r where r.id = :reviewId and r.deletedAt is null")
    Optional<Review> findActiveByIdForUpdate(final Long reviewId);

    boolean existsByUserIdAndReviewKey(final Long userId, final String reviewKey);

    Optional<Review> findByIdAndUserIdAndDeletedAtIsNull(final Long reviewId, final Long userId);
}
