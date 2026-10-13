package com.book.core.review.application.port;

import com.book.core.review.domain.Review;
import com.book.common.paging.OffsetLimit;
import com.book.common.paging.Page;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.domain.ReviewTarget;
import java.util.Optional;
import java.util.List;

public interface ReviewRepositoryPort {
    Page<ReviewItemResult> findActiveReviews(final ReviewTarget target, final OffsetLimit offsetLimit);

    List<String> findUsedReviewKeys(final Long userId, final List<String> reviewKeys);

    Review save(final Review review);

    Optional<Review> findActiveById(final Long reviewId);

    boolean existsByUserIdAndReviewKey(final Long userId, final String reviewKey);

    List<Review> findByUserIdAndReviewKeyIn(final Long userId, final List<String> reviewKeys);

    Optional<Review> findByIdAndUserId(Long reviewId, Long userId);
}
