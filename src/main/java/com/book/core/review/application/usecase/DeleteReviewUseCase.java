package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.review.application.ReviewHistoryRecorder;
import com.book.core.review.domain.enums.ReviewOperation;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewFormat;
import com.book.core.review.application.result.ReviewProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class DeleteReviewUseCase {
    private final ReviewRepositoryPort reviewRepository;
    private final ReviewHistoryRecorder historyRecorder;

    @Transactional
    public ReviewProcessResult execute(final Long userId, final Long reviewId, final String requestIp) {
        log.debug("리뷰 삭제 처리를 시작합니다. reviewId={}, traceId={}", reviewId, MDC.get("traceId"));
        final Review review =
            reviewRepository.findActiveByIdForUpdate(reviewId).orElseThrow(() -> new CoreException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.userId().equals(userId)) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        review.delete();
        reviewRepository.save(review);
        historyRecorder.record(review, ReviewOperation.DELETE, requestIp);
        log.info("리뷰 삭제 처리를 마쳤습니다. reviewId={}, traceId={}", reviewId, MDC.get("traceId"));
        return new ReviewProcessResult(review.id(), ReviewFormat.TEXT);
    }
}
