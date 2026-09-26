package com.book.core.review.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Review extends BaseEntity {
    private static final BigDecimal MAX_RATING = new BigDecimal("10.0");
    private static final int MAX_CONTENT_LENGTH = 255;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(nullable = false, precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "is_spoiler", nullable = false)
    private boolean isSpoiler;

    private Review(final Long userId, final Long orderItemId, final BigDecimal rating, final String content, final boolean isSpoiler) {
        if (userId == null || userId <= 0 || orderItemId == null || orderItemId <= 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        this.userId = userId;
        this.orderItemId = orderItemId;
        this.rating = normalizeRating(rating);
        this.content = validateContent(content);
        this.isSpoiler = isSpoiler;
    }

    public static Review create(final Long userId, final Long orderItemId, final BigDecimal rating, final String content,
        final boolean isSpoiler) {
        return new Review(userId, orderItemId, rating, content, isSpoiler);
    }

    public void update(final BigDecimal rating, final String content, final Boolean isSpoiler) {
        final BigDecimal updatedRating = rating == null ? this.rating : normalizeRating(rating);
        final String updatedContent = content == null ? this.content : validateContent(content);
        this.rating = updatedRating;
        this.content = updatedContent;
        if (isSpoiler != null) {
            this.isSpoiler = isSpoiler;
        }
    }

    private static BigDecimal normalizeRating(final BigDecimal rating) {
        if (rating == null || rating.signum() < 0 || rating.compareTo(MAX_RATING) > 0) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        try {
            return rating.setScale(1, RoundingMode.UNNECESSARY);
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
