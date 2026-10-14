package com.book.core.review.api.converter;

import com.book.core.review.api.request.AddReviewRequest;
import com.book.core.review.api.request.UpdateReviewRequest;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.ReviewContent;
import org.springframework.stereotype.Component;
import com.book.common.paging.OffsetLimit;
import com.book.core.review.domain.enums.ReviewTargetType;

@Component
public class ReviewCommandConverter {
    public GetReviewsCommand toGetReviewsCommand(final ReviewTargetType targetType, final Long targetId, final int offset,
        final int limit) {
        return new GetReviewsCommand(new ReviewTarget(targetType, targetId), new OffsetLimit(offset, limit));
    }

    public AddReviewCommand toAddReviewCommand(final Long userId, final AddReviewRequest request) {
        return new AddReviewCommand(userId, new ReviewTarget(request.targetType(), request.targetId()),
            new ReviewContent(request.rate(), request.content()), request.isSpoiler());
    }

    public UpdateReviewCommand toUpdateReviewCommand(final Long userId, final Long reviewId, final UpdateReviewRequest request) {
        return new UpdateReviewCommand(userId, reviewId, new ReviewContent(request.rate(), request.content()), request.isSpoiler());
    }

    public RemoveReviewCommand toRemoveReviewCommand(final Long userId, final Long reviewId) {
        return new RemoveReviewCommand(userId, reviewId);
    }
}
