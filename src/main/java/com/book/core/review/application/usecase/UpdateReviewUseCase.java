package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewContent;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateReviewUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional
    public void execute(final UpdateReviewCommand command) {
        final Review review =
            reviewRepository.findActiveById(command.reviewId()).orElseThrow(() -> new CoreException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.userId().equals(command.userId())) {
            throw new CoreException(ErrorCode.FORBIDDEN);
        }
        BigDecimal rate = review.rate();
        if (command.rating() != null) {
            rate = command.rating();
        }
        String content = review.content();
        if (command.content() != null) {
            content = command.content();
        }
        review.updateContent(new ReviewContent(rate, content), command.isSpoiler());
        reviewRepository.save(review);
    }
}
