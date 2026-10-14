package com.book.core.review.domain;

import com.book.common.domain.BaseEntity;
import com.book.core.review.domain.enums.ReviewTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

    @Column(name = "rate", nullable = false, precision = 3, scale = 1)
    private BigDecimal rate;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Column(name = "is_spoiler", nullable = false)
    private boolean isSpoiler;

    public Review(final Long id, final Long userId, final String reviewKey, final ReviewTargetType targetType, final Long targetId,
        final BigDecimal rate, final String content, final boolean isSpoiler) {
        super(id);
        this.userId = userId;
        this.reviewKey = reviewKey;
        this.targetType = targetType;
        this.targetId = targetId;
        final ReviewContent reviewContent = new ReviewContent(rate, content);
        this.rate = reviewContent.rate();
        this.content = reviewContent.content();
        this.isSpoiler = isSpoiler;
    }

    public static Review create(final ReviewKey key, final ReviewTarget target, final ReviewContent content, final boolean isSpoiler) {
        return new Review(null, key.userId(), key.key(), target.type(), target.id(), content.rate(), content.content(), isSpoiler);
    }

    /** 표현상의 소수 자릿수 차이는 무시하고 실제 리뷰 상태의 변경 여부를 확인합니다. */
    public boolean wouldChange(final ReviewContent content, final Boolean isSpoiler) {
        return this.rate.compareTo(content.rate()) != 0 || !this.content.equals(content.content())
            || (isSpoiler != null && this.isSpoiler != isSpoiler);
    }

    public void updateContent(final ReviewContent content, final Boolean isSpoiler) {
        this.rate = content.rate();
        this.content = content.content();
        if (isSpoiler != null) {
            this.isSpoiler = isSpoiler;
        }
    }

}
