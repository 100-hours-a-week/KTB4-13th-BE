package com.book.core.review.application.service;

import com.book.core.review.application.command.CreateReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.common.paging.Page;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.result.ReviewProcessResult;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.usecase.CreateReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.GetReviewsUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final CreateReviewUseCase createReviewUseCase;
    private final UpdateReviewUseCase updateReviewUseCase;
    private final DeleteReviewUseCase deleteReviewUseCase;
    private final GetReviewsUseCase getReviewsUseCase;
    private final ValidateReviewPolicyUseCase validateReviewPolicyUseCase;

    public Page<ReviewItemResult> findReviews(final GetReviewsCommand command) {
        return getReviewsUseCase.execute(command.target(), command.offsetLimit());
    }

    public void createReview(final CreateReviewCommand command) {
        createReviewUseCase.execute(command);
    }

    public Long updateReview(final UpdateReviewCommand command) {
        validateReviewPolicyUseCase.validateUpdate(command.userId(), command.reviewId());
        return updateReviewUseCase.execute(command.userId(), command.reviewId(), command.content(), command.isSpoiler());
    }

    public Long removeReview(final RemoveReviewCommand command) {
        final ReviewProcessResult result = deleteReviewUseCase.execute(command.userId(), command.reviewId());
        return result.id();
    }
}
