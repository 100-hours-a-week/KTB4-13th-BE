package com.book.core.review.application.service;

import com.book.common.paging.Page;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.domain.enums.PointType;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.application.result.ReviewProcessResult;
import com.book.core.review.application.usecase.GetReviewsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewWriteService reviewWriteService;
    private final GetReviewsUseCase getReviewsUseCase;
    private final EarnPointUseCase earnPointUseCase;
    private final DeductPointUseCase deductPointUseCase;

    public Page<ReviewItemResult> findReviews(final GetReviewsCommand command) {
        return getReviewsUseCase.execute(command.target(), command.offsetLimit());
    }

    public Long addReview(final AddReviewCommand command) {
        final ReviewProcessResult result = reviewWriteService.addReview(command);
        earnPointUseCase.execute(command.userId(), PointType.REVIEW, result.id(), result.pointAmount());
        return result.id();
    }

    public Long updateReview(final UpdateReviewCommand command) {
        return reviewWriteService.updateReview(command);
    }

    public Long removeReview(final RemoveReviewCommand command) {
        final ReviewProcessResult result = reviewWriteService.removeReview(command);
        deductPointUseCase.execute(command.userId(), PointType.REVIEW, result.id(), result.pointAmount());
        return result.id();
    }
}
