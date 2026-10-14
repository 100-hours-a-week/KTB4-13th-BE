package com.book.core.review.application.service;

import com.book.common.paging.Page;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.enums.PointType;
import com.book.core.review.application.ReviewHistoryRecorder;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.application.result.ReviewProcessResult;
import com.book.core.review.application.usecase.AddReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.GetReviewsUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewFormat;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.enums.ReviewEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final AddReviewUseCase addReviewUseCase;
    private final UpdateReviewUseCase updateReviewUseCase;
    private final DeleteReviewUseCase deleteReviewUseCase;
    private final GetReviewsUseCase getReviewsUseCase;
    private final ValidateReviewPolicyUseCase validateReviewPolicyUseCase;
    private final EarnPointUseCase earnPointUseCase;
    private final DeductPointUseCase deductPointUseCase;
    private final ReviewHistoryRecorder historyRecorder;
    private final TransactionTemplate transactionTemplate;

    public Page<ReviewItemResult> findReviews(final GetReviewsCommand command) {
        return getReviewsUseCase.execute(command.target(), command.offsetLimit());
    }

    public Long addReview(final AddReviewCommand command) {
        final ReviewKey reviewKey = validateReviewPolicyUseCase.validateNew(command.userId(), command.target());
        final ReviewProcessResult result = transactionTemplate.execute((final var status) -> {
            final Review review = addReviewUseCase.execute(command.target(), reviewKey, command.content());
            historyRecorder.record(review, ReviewEventType.CREATE);
            return new ReviewProcessResult(review.id(), ReviewFormat.TEXT);
        });
        earnPointUseCase.execute(command.userId(), PointType.REVIEW, result.id(), result.pointAmount());
        return result.id();
    }

    public Long updateReview(final UpdateReviewCommand command) {
        validateReviewPolicyUseCase.validateUpdate(command.userId(), command.reviewId());
        return transactionTemplate.execute((final var status) -> {
            updateReviewUseCase.execute(command.userId(), command.reviewId(), command.content())
                .ifPresent((final var review) -> historyRecorder.record(review, ReviewEventType.UPDATE));
            return command.reviewId();
        });
    }

    public Long removeReview(final RemoveReviewCommand command) {
        final ReviewProcessResult result = transactionTemplate.execute((final var status) -> {
            final Review review = deleteReviewUseCase.execute(command.userId(), command.reviewId());
            historyRecorder.record(review, ReviewEventType.DELETE);
            return new ReviewProcessResult(review.id(), ReviewFormat.TEXT);
        });
        deductPointUseCase.execute(command.userId(), PointType.REVIEW, result.id(), result.pointAmount());
        return result.id();
    }
}
