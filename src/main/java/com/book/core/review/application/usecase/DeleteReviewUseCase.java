package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.command.DeleteReviewCommand;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteReviewUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional
    public void execute(final DeleteReviewCommand command) {
        final Review review =
            reviewRepository.findActiveById(command.reviewId()).orElseThrow(() -> new CoreException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.userId().equals(command.userId())) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        review.delete();
        reviewRepository.save(review);
    }
}
