package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewContent;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateReviewUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional
    public Long execute(final Long userId, final Long reviewId, final ReviewContent content, final Boolean isSpoiler) {
        final Review review = reviewRepository.findActiveById(reviewId).orElseThrow(() -> new CoreException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.userId().equals(userId)) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        review.updateContent(content, isSpoiler);
        reviewRepository.save(review);
        return review.id();
    }
}
