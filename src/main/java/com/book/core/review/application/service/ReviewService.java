package com.book.core.review.application.service;

import com.book.common.paging.Page;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.enums.PointType;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.GetReviewCountCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.ReviewHistoryRecorder;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.application.result.ReviewProcessResult;
import com.book.core.review.application.usecase.AddReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.GetReviewsUseCase;
import com.book.core.review.application.usecase.GetReviewCountUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
import com.book.core.review.domain.ReviewFormat;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.enums.ReviewEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final GetReviewsUseCase getReviewsUseCase;
    private final GetReviewCountUseCase getReviewCountUseCase;
    private final AddReviewUseCase addReviewUseCase;
    private final UpdateReviewUseCase updateReviewUseCase;
    private final DeleteReviewUseCase deleteReviewUseCase;
    private final ValidateReviewPolicyUseCase validateReviewPolicyUseCase;
    private final ReviewHistoryRecorder historyRecorder;
    private final EarnPointUseCase earnPointUseCase;
    private final DeductPointUseCase deductPointUseCase;

    public Page<ReviewItemResult> findReviews(final GetReviewsCommand command) {
        return getReviewsUseCase.execute(command.target(), command.offsetLimit());
    }

    public long countReviews(final GetReviewCountCommand command) {
        return getReviewCountUseCase.execute(command.target());
    }

    @Transactional
    public Long addReview(final AddReviewCommand command) {
        final ReviewKey reviewKey = validateReviewPolicyUseCase.validateNew(command.userId(), command.target());
        final Long reviewId = addReviewUseCase.execute(command.target(), reviewKey, command.content());
        historyRecorder.record(reviewId, ReviewEventType.CREATE);
        final ReviewProcessResult result = new ReviewProcessResult(reviewId, ReviewFormat.TEXT);
        earnPointUseCase.execute(command.userId(), PointType.REVIEW, result.id(), result.pointAmount());
        return result.id();
    }

    @Transactional
    public Long updateReview(final UpdateReviewCommand command) {
        validateReviewPolicyUseCase.validateUpdate(command.userId(), command.reviewId());
        final Long reviewId = updateReviewUseCase.execute(command.userId(), command.reviewId(), command.content());
        historyRecorder.record(reviewId, ReviewEventType.UPDATE);
        return reviewId;
    }

    @Transactional
    public Long removeReview(final RemoveReviewCommand command) {
        final Long reviewId = deleteReviewUseCase.execute(command.userId(), command.reviewId());
        historyRecorder.record(reviewId, ReviewEventType.DELETE);
        final ReviewProcessResult result = new ReviewProcessResult(reviewId, ReviewFormat.TEXT);
        deductPointUseCase.execute(command.userId(), PointType.REVIEW, result.id(), result.pointAmount());
        return result.id();
    }
}
