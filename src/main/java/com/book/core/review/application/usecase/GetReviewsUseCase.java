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
        final Page<ReviewItemResult> result = reviewRepository.findActiveReviews(target, offsetLimit);
        log.info("리뷰를 조회했습니다. targetType={}, targetId={}, offset={}, limit={}, count={}, hasNext={}, traceId={}", target.type(),
            target.id(), offsetLimit.offset(), offsetLimit.limit(), result.content().size(), result.hasNext(), MDC.get("traceId"));
        return result;
    }
}
