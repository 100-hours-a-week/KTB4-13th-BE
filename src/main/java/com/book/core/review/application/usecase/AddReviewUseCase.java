package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
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
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class AddReviewUseCase {
    private final ReviewRepositoryPort reviewRepository;

    @Transactional
    public ReviewProcessResult execute(final ReviewTarget target, final ReviewKey reviewKey, final ReviewContent content,
        final boolean isSpoiler) {
        if (reviewRepository.existsByUserIdAndReviewKey(reviewKey.userId(), reviewKey.key())) {
            throw new CoreException(ErrorCode.REVIEW_ALREADY_REVIEWED);
        }
        final Review saved = reviewRepository.save(Review.create(reviewKey, target, content, isSpoiler));
        return new ReviewProcessResult(saved.id(), ReviewFormat.TEXT);
    }
}
