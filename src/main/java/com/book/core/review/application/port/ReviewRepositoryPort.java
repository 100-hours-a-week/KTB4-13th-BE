package com.book.core.review.application.port;

import com.book.core.review.domain.Review;
import java.util.Optional;
import java.util.List;

public interface ReviewRepositoryPort {

    List<String> findUsedReviewKeys(final Long userId, final List<String> reviewKeys);

    Review save(final Review review);

    Optional<Review> findActiveById(final Long reviewId);

    boolean existsByUserIdAndReviewKey(final Long userId, final String reviewKey);

    List<Review> findByUserIdAndReviewKeyIn(final Long userId, final List<String> reviewKeys);

    Optional<Review> findByIdAndUserId(Long reviewId, Long userId);
}
