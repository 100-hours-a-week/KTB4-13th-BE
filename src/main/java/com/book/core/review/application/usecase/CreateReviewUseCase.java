package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.order.application.usecase.GetOrderItemUseCase;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderItemStatus;
import com.book.core.review.application.command.CreateReviewCommand;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.ReviewContent;
import com.book.core.review.domain.enums.ReviewTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateReviewUseCase {
    private final ReviewRepositoryPort reviewRepository;
    private final GetOrderItemUseCase getOrderItemUseCase;

    @Transactional
    public void execute(final CreateReviewCommand command) {
        final OrderItem orderItem = getOrderItemUseCase.execute(command.orderItemId());
        if (!orderItem.order().userId().equals(command.userId()) || orderItem.status() != OrderItemStatus.PAID) {
            throw new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER);
        }
        if (reviewRepository.existsByUserIdAndReviewKey(command.userId(), "ORDER_ITEM_" + command.orderItemId())) {
            throw new CoreException(ErrorCode.REVIEW_ALREADY_REVIEWED);
        }
        reviewRepository.save(Review.create(new ReviewKey(command.userId(), "ORDER_ITEM_" + command.orderItemId()),
            new ReviewTarget(ReviewTargetType.PRODUCT, orderItem.productId()), new ReviewContent(command.rating(), command.content()),
            command.isSpoiler()));
    }
}
