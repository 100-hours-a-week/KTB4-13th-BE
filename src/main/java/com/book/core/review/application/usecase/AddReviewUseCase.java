package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.ReviewContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class AddReviewUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional
    public Long execute(final ReviewTarget target, final ReviewKey reviewKey, final ReviewContent content) {
        log.debug("리뷰 작성 처리를 시작합니다. targetType={}, targetId={}, traceId={}", target.type(), target.id(), MDC.get("traceId"));
        if (reviewRepository.existsByUserIdAndReviewKey(reviewKey.userId(), reviewKey.key())) {
            throw new CoreException(ErrorCode.REVIEW_ALREADY_REVIEWED);
        }
        final Long reviewId = reviewRepository.save(Review.create(reviewKey, target, content)).id();
        log.info("리뷰 작성 처리를 마쳤습니다. targetType={}, targetId={}, traceId={}", target.type(), target.id(), MDC.get("traceId"));
        return reviewId;
    }
}
