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
    private final ReviewHistoryRecorder historyRecorder;

    @Transactional
    public ReviewProcessResult execute(final ReviewTarget target, final ReviewKey reviewKey, final ReviewContent content,
        final boolean isSpoiler, final String requestIp) {
        log.debug("리뷰 작성 처리를 시작합니다. targetType={}, targetId={}, traceId={}", target.type(), target.id(), MDC.get("traceId"));
        if (reviewRepository.existsByUserIdAndReviewKey(reviewKey.userId(), reviewKey.key())) {
            throw new CoreException(ErrorCode.REVIEW_ALREADY_REVIEWED);
        }
        final Review saved = reviewRepository.save(Review.create(reviewKey, target, content, isSpoiler));
        historyRecorder.record(saved, ReviewOperation.CREATE, requestIp);
        log.info("리뷰 작성 처리를 마쳤습니다. targetType={}, targetId={}, traceId={}", target.type(), target.id(), MDC.get("traceId"));
        return new ReviewProcessResult(saved.id(), ReviewFormat.TEXT);
    }
}
