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

    long countActiveReviews(final ReviewTarget target);

    List<String> findUsedReviewKeys(final Long userId, final List<String> reviewKeys);

    Review save(final Review review);

    Optional<Review> findActiveById(final Long reviewId);

    Optional<Review> findByIdIncludingDeleted(final Long reviewId);

    /** 동시 수정·삭제가 오래된 상태를 이력에 남기지 않도록 리뷰 행을 먼저 잠가 최신 상태를 읽습니다. */
    Optional<Review> findActiveByIdWithLock(final Long reviewId);

    boolean existsByUserIdAndReviewKey(final Long userId, final String reviewKey);

    List<Review> findByUserIdAndReviewKeyIn(final Long userId, final List<String> reviewKeys);

    Optional<Review> findByIdAndUserId(Long reviewId, Long userId);
}
