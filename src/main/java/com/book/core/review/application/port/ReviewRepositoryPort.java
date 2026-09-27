package com.book.core.review.application.port;

import com.book.core.review.domain.Review;
import java.util.Optional;

public interface ReviewRepositoryPort {
    Review save(final Review review);

    Optional<Review> findActiveById(final Long reviewId);

    boolean existsActiveByUserIdAndOrderItemId(final Long userId, final Long orderItemId);
}
