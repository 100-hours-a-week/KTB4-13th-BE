package com.book.core.review.domain;

import com.book.core.review.domain.enums.ReviewEventType;
import com.book.common.domain.BaseTimeEntity;
import com.book.core.review.domain.enums.ReviewTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "review_history",
    indexes = {@Index(name = "idx_review_history_review_order", columnList = "review_id,changed_at,id"),
        @Index(name = "idx_review_history_user", columnList = "user_id")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class ReviewHistory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false, updatable = false)
    private Long reviewId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false, length = 16)
    private ReviewTargetType targetType;

    @Column(name = "target_id", nullable = false, updatable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 16)
    private ReviewEventType eventType;

    @Column(nullable = false, updatable = false, length = 255)
    private String content;

    @Column(nullable = false, updatable = false, precision = 3, scale = 1)
    private BigDecimal rate;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    public ReviewHistory(final Review review, final ReviewEventType eventType, final LocalDateTime changedAt) {
        this.reviewId = review.id();
        this.userId = review.userId();
        this.targetType = review.targetType();
        this.targetId = review.targetId();
        this.eventType = eventType;
        this.content = review.content();
        this.rate = review.rate();
        this.changedAt = changedAt;
    }
}
