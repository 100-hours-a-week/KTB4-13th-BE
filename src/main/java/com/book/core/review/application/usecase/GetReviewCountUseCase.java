package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.ReviewTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetReviewCountUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional(readOnly = true)
    public long execute(final ReviewTarget target) {
        return reviewRepository.countActiveReviews(target);
    }
}
