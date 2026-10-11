package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.result.GetReviewsResult;
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
    public GetReviewsResult execute(final GetReviewsCommand command) {
        log.debug("리뷰 조회를 시작합니다. targetType={}, targetId={}, offset={}, limit={}, traceId={}", command.targetType(), command.targetId(),
            command.offsetLimit().offset(), command.offsetLimit().limit(), MDC.get("traceId"));
        final var page = reviewRepository.findActiveReviews(command.targetType(), command.targetId(), command.offsetLimit());
        return new GetReviewsResult(page.content(), page.hasNext());
    }
}
