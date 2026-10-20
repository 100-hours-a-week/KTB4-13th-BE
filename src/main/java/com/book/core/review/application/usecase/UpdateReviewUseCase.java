package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpdateReviewUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional
    public Long execute(final Long userId, final Long reviewId, final ReviewContent content) {
        log.atDebug().addKeyValue("reviewId", reviewId).log("[REVIEW] 리뷰 수정 처리를 시작합니다. reviewId={}", reviewId);
        final Review review =
            reviewRepository.findActiveByIdWithLock(reviewId).orElseThrow(() -> new CoreException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.userId().equals(userId)) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        review.updateContent(content);
        reviewRepository.save(review);
        log.atInfo().addKeyValue("reviewId", reviewId).log("[REVIEW] 리뷰 수정 처리를 마쳤습니다. reviewId={}", reviewId);
        return review.id();
    }
}
