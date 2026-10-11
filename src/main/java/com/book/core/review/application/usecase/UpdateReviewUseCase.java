package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpdateReviewUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional
    public void execute(final UpdateReviewCommand command) {
        log.debug("리뷰 수정 처리를 시작합니다. reviewId={}, traceId={}", command.reviewId(), MDC.get("traceId"));
        final Review review =
            reviewRepository.findActiveById(command.reviewId()).orElseThrow(() -> new CoreException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.userId().equals(command.userId())) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        review.update(command.rating(), command.content(), command.isSpoiler());
        reviewRepository.save(review);
        log.info("리뷰 수정 처리를 마쳤습니다. reviewId={}, traceId={}", command.reviewId(), MDC.get("traceId"));
    }
}
