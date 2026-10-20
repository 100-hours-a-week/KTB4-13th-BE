package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class DeleteReviewUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional
    public Long execute(final Long userId, final Long reviewId) {
        log.debug("리뷰 삭제 처리를 시작합니다. reviewId={}, requestId={}", reviewId, MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY));
        final Review review =
            reviewRepository.findActiveByIdWithLock(reviewId).orElseThrow(() -> new CoreException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.userId().equals(userId)) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        review.delete();
        reviewRepository.save(review);
        log.info("리뷰 삭제 처리를 마쳤습니다. reviewId={}, requestId={}", reviewId, MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY));
        return review.id();
    }
}
