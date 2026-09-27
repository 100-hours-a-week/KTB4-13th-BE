package com.book.core.review.api.converter;

import com.book.core.review.api.request.CreateReviewRequest;
import com.book.core.review.api.request.UpdateReviewRequest;
import com.book.core.review.application.command.CreateReviewCommand;
import com.book.core.review.application.command.DeleteReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import org.springframework.stereotype.Component;

@Component
public class ReviewCommandConverter {
    public CreateReviewCommand toCreateReviewCommand(final CreateReviewRequest request) {
        return new CreateReviewCommand(request.userId(), request.targetId(), request.rate(), request.content(), request.isSpoiler());
    }

    public UpdateReviewCommand toUpdateReviewCommand(final Long reviewId, final UpdateReviewRequest request) {
        return new UpdateReviewCommand(reviewId, request.rating(), request.content(), request.isSpoiler());
    }

    public DeleteReviewCommand toDeleteReviewCommand(final Long reviewId) {
        return new DeleteReviewCommand(reviewId);
    }
}
