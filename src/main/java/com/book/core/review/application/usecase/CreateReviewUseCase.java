package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.ValidateReviewableOrderItemUseCase;
import com.book.core.review.application.command.CreateReviewCommand;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateReviewUseCase {
    private final ReviewRepositoryPort reviewRepository;
    private final ValidateReviewableOrderItemUseCase validateReviewableOrderItem;

    @Transactional
    public void execute(final CreateReviewCommand command) {
        validateReviewableOrderItem.execute(command.userId(), command.orderItemId());
        if (reviewRepository.existsActiveByUserIdAndOrderItemId(command.userId(), command.orderItemId())) {
            throw new CoreException(ErrorCode.REVIEW_ALREADY_REVIEWED);
        }
        reviewRepository
            .save(Review.create(command.userId(), command.orderItemId(), command.rating(), command.content(), command.isSpoiler()));
    }
}
