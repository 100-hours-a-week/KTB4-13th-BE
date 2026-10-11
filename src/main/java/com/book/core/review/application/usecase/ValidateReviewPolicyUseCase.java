package com.book.core.review.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.application.port.ReviewOrderRepositoryPort;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.application.result.ReviewKeyResult;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ValidateReviewPolicyUseCase {
    private final ReviewOrderRepositoryPort orderRepository;
    private final ReviewRepositoryPort reviewRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ReviewKeyResult validateNew(final Long userId, final ReviewTargetType targetType, final Long targetId) {
        if (userId == null || userId <= 0 || targetType != ReviewTargetType.PRODUCT || targetId == null || targetId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        final var orderItemIds = orderRepository.findRecentPaidOrderItemIds(userId, targetId, LocalDateTime.now(clock).minusDays(14));
        if (orderItemIds.isEmpty()) {
            throw new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER);
        }
        final var keys = orderItemIds.stream().map(id -> "ORDER_ITEM_" + id).toList();
        final var usedKeys = new HashSet<>(reviewRepository.findUsedReviewKeys(userId, keys));
        final String key = keys.stream().filter(candidate -> !usedKeys.contains(candidate)).findFirst()
            .orElseThrow(() -> new CoreException(ErrorCode.REVIEW_HAS_NOT_ORDER));
        return new ReviewKeyResult(userId, key);
    }

    @Transactional(readOnly = true)
    public void validateUpdate(final Long userId, final Long reviewId) {
        if (userId == null || userId <= 0 || reviewId == null || reviewId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        final var review = reviewRepository.findActiveById(reviewId).filter(found -> found.userId().equals(userId))
            .orElseThrow(() -> new CoreException(ErrorCode.REVIEW_NOT_FOUND));
        if (review.createdAt().plusDays(7).isBefore(LocalDateTime.now(clock))) {
            throw new CoreException(ErrorCode.REVIEW_UPDATE_EXPIRED);
        }
    }
}
