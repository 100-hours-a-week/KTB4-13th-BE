package com.book.core.review.application.port;

import com.book.core.review.domain.Review;
import com.book.common.paging.OffsetLimit;
import com.book.common.paging.Page;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.util.Optional;

public interface ReviewRepositoryPort {
    Page<ReviewItemResult> findActiveReviews(final ReviewTargetType targetType, final Long targetId, final OffsetLimit offsetLimit);

    Review save(final Review review);

    Optional<Review> findActiveById(final Long reviewId);

    boolean existsActiveByUserIdAndOrderItemId(final Long userId, final Long orderItemId);
}
