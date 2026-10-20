package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.port.ReviewOrderItemRepositoryPort;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewKey;
import com.book.core.review.domain.ReviewTarget;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ValidateReviewPolicyUseCase {
    private final ReviewOrderItemRepositoryPort orderItemRepository;
    private final ReviewRepositoryPort reviewRepository;

    @Transactional(readOnly = true)
    public ReviewKey validateNew(final Long userId, final ReviewTarget target) {
        switch (target.type()) {
            case PRODUCT -> {
                final var ids = orderItemRepository.findConfirmedOrderItemsForProduct(userId, target.id());
                if (ids.isEmpty()) {
                    throw new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER);
                }
                final var keys = ids.stream().map(id -> "ORDER_ITEM_" + id).toList();
                final var used =
                    new HashSet<>(reviewRepository.findByUserIdAndReviewKeyIn(userId, keys).stream().map(Review::reviewKey).toList());
                final String key = keys.stream().filter(candidate -> !used.contains(candidate)).findFirst()
                    .orElseThrow(() -> new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER));
                return new ReviewKey(userId, key);
            }
        }
        throw new UnsupportedOperationException(); // NOTE: 다른 ReviewTargetType에 대한 정책 추가
    }

    @Transactional(readOnly = true)
    public void validateUpdate(final Long userId, final Long reviewId) {
        reviewRepository.findByIdAndUserId(reviewId, userId).filter(found -> found.userId().equals(userId))
            .orElseThrow(() -> new CoreException(ErrorCode.REVIEW_NOT_FOUND));
    }
}
