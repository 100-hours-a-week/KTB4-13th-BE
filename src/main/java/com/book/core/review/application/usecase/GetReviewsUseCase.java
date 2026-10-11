package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.result.GetReviewsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetReviewsUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional(readOnly = true)
    public GetReviewsResult execute(final GetReviewsCommand command) {
        final var page = reviewRepository.findActiveReviews(command.targetType(), command.targetId(), command.offsetLimit());
        return new GetReviewsResult(page.content(), page.hasNext());
    }
}
