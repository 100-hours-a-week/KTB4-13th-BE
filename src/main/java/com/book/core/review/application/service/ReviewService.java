package com.book.core.review.application.service;

import com.book.core.review.application.command.CreateReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.result.GetReviewsResult;
import com.book.core.review.application.command.DeleteReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.usecase.CreateReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.GetReviewsUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
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

    public GetReviewsResult findReviews(final GetReviewsCommand command) {
        return getReviewsUseCase.execute(command);
    }

    public void createReview(final CreateReviewCommand command) {
        createReviewUseCase.execute(command);
    }

    public void updateReview(final UpdateReviewCommand command) {
        updateReviewUseCase.execute(command);
    }

    public void deleteReview(final DeleteReviewCommand command) {
        deleteReviewUseCase.execute(command);
    }
}
