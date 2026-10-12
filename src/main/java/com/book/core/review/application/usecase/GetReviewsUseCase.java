package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.paging.OffsetLimit;
import com.book.common.paging.Page;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.domain.ReviewTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetReviewsUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional(readOnly = true)
    public Page<ReviewItemResult> execute(final ReviewTarget target, final OffsetLimit offsetLimit) {
        log.debug("리뷰 조회 처리를 시작합니다. targetType={}, targetId={}, offset={}, limit={}, traceId={}", target.type(), target.id(),
            offsetLimit.offset(), offsetLimit.limit(), MDC.get("traceId"));
        final Page<ReviewItemResult> result = reviewRepository.findActiveReviews(target, offsetLimit);
        return result;
    }
}
