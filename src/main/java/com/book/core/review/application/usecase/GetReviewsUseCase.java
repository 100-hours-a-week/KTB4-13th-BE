package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.paging.OffsetLimit;
import com.book.common.paging.Page;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.domain.ReviewTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetReviewsUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional(readOnly = true)
    public Page<ReviewItemResult> execute(final ReviewTarget target, final OffsetLimit offsetLimit) {
        final Page<ReviewItemResult> result = reviewRepository.findActiveReviews(target, offsetLimit);
        return result;
    }
}
