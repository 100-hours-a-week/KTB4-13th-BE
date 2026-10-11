package com.book.core.review.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.domain.enums.ReviewTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "reviews", indexes = {@Index(name = "udx_user_review", columnList = "user_id,review_key", unique = true)})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Review extends BaseEntity {
    private static final BigDecimal MAX_RATING = new BigDecimal("10.0");
    private static final int MAX_CONTENT_LENGTH = 255;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "review_key", nullable = false, length = 64)
    private String reviewKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private ReviewTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "rating", nullable = false, precision = 3, scale = 1)
    private BigDecimal rate;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "is_spoiler", nullable = false)
    private boolean isSpoiler;

    public Review(final Long id, final Long userId, final String reviewKey, final ReviewTargetType targetType, final Long targetId,
        final Long orderItemId, final BigDecimal rate, final String content, final boolean isSpoiler) {
        super(id);
        if (userId == null || userId <= 0 || reviewKey == null || reviewKey.isBlank() || reviewKey.length() > 64 || targetType == null
            || targetId == null || targetId <= 0 || orderItemId == null || orderItemId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
        this.reviewKey = reviewKey;
        this.targetType = targetType;
        this.targetId = targetId;
        this.orderItemId = orderItemId;
        this.rate = normalizeRating(rate);
        this.content = validateContent(content);
        this.isSpoiler = isSpoiler;
    }

    public static Review create(final Long userId, final Long orderItemId, final Long productId, final BigDecimal rate,
        final String content, final boolean isSpoiler) {
        return new Review(null, userId, "ORDER_ITEM_" + orderItemId, ReviewTargetType.PRODUCT, productId, orderItemId, rate, content,
            isSpoiler);
    }

    public void update(final BigDecimal rate, final String content, final Boolean isSpoiler) {
        final BigDecimal updatedRate = rate == null ? this.rate : normalizeRating(rate);
        final String updatedContent = content == null ? this.content : validateContent(content);
        this.rate = updatedRate;
        this.content = updatedContent;
        if (isSpoiler != null) {
            this.isSpoiler = isSpoiler;
        }
    }

    public void updateContent(final BigDecimal rate, final String content) {
        update(rate, content, null);
    }

    private static BigDecimal normalizeRating(final BigDecimal rate) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(MAX_RATING) > 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        try {
            return rate.setScale(1, RoundingMode.UNNECESSARY);
        } catch (final ArithmeticException exception) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static String validateContent(final String content) {
        if (content == null || content.length() > MAX_CONTENT_LENGTH) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        return content;
    }
}
