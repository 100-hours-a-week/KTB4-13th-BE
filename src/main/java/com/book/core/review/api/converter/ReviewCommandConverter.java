package com.book.core.review.api.converter;

import com.book.core.review.api.request.CreateReviewRequest;
import com.book.core.review.api.request.UpdateReviewRequest;
import com.book.core.review.application.command.CreateReviewCommand;
import com.book.core.review.application.command.DeleteReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import org.springframework.stereotype.Component;
import com.book.common.paging.OffsetLimit;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.domain.enums.ReviewTargetType;
import com.book.core.review.domain.ReviewTarget;

@Component
public class ReviewCommandConverter {
    public GetReviewsCommand toGetReviewsCommand(final ReviewTargetType targetType, final Long targetId, final int offset,
        final int limit) {
        return new GetReviewsCommand(new ReviewTarget(targetType, targetId), new OffsetLimit(offset, limit));
    }

    public CreateReviewCommand toCreateReviewCommand(final Long userId, final CreateReviewRequest request) {
        return new CreateReviewCommand(userId, request.targetId(), request.rate(), request.content(), request.isSpoiler());
    }

    public UpdateReviewCommand toUpdateReviewCommand(final Long userId, final Long reviewId, final UpdateReviewRequest request) {
        return new UpdateReviewCommand(userId, reviewId, request.rating(), request.content(), request.isSpoiler());
    }

    public DeleteReviewCommand toDeleteReviewCommand(final Long userId, final Long reviewId) {
        return new DeleteReviewCommand(userId, reviewId);
    }
}
