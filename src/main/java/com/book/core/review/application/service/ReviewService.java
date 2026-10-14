package com.book.core.review.application.service;

import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.domain.enums.PointType;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.common.paging.Page;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.application.result.ReviewProcessResult;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.usecase.AddReviewUseCase;
import com.book.core.review.application.usecase.DeleteReviewUseCase;
import com.book.core.review.application.usecase.GetReviewsUseCase;
import com.book.core.review.application.usecase.UpdateReviewUseCase;
import com.book.core.review.application.usecase.ValidateReviewPolicyUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public Page<ReviewItemResult> findReviews(final GetReviewsCommand command) {
        return getReviewsUseCase.execute(command.target(), command.offsetLimit());
    }

    public Long addReview(final AddReviewCommand command) {
        final ReviewKey reviewKey = validateReviewPolicyUseCase.validateNew(command.userId(), command.target());
        final ReviewProcessResult result =
            addReviewUseCase.execute(command.target(), reviewKey, command.content(), command.isSpoiler(), command.requestIp());
        earnPointUseCase.execute(command.userId(), PointType.REVIEW, result.id(), result.pointAmount());
        return result.id();
    }

    public Long updateReview(final UpdateReviewCommand command) {
        validateReviewPolicyUseCase.validateUpdate(command.userId(), command.reviewId());
        return updateReviewUseCase.execute(command.userId(), command.reviewId(), command.content(), command.isSpoiler(),
            command.requestIp());
    }

    public Long removeReview(final RemoveReviewCommand command) {
        final ReviewProcessResult result = deleteReviewUseCase.execute(command.userId(), command.reviewId(), command.requestIp());
        deductPointUseCase.execute(command.userId(), PointType.REVIEW, result.id(), result.pointAmount());
        return result.id();
    }
}
