package com.book.core.review.domain;

import com.book.core.review.domain.enums.ReviewOperation;
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

/** 수정 가능한 생명주기 없이, 작업 직후의 리뷰 내용을 보존합니다. */
@Entity
@Table(name = "review_history",
    indexes = {@Index(name = "idx_review_history_review_order", columnList = "review_id,changed_at,id"),
        @Index(name = "idx_review_history_user", columnList = "user_id")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class ReviewHistory {
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
    @Column(nullable = false, updatable = false, length = 16)
    private ReviewOperation operation;

    @Column(nullable = false, updatable = false, length = 255)
    private String content;

    @Column(nullable = false, updatable = false, precision = 3, scale = 1)
    private BigDecimal rate;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "request_ip", nullable = false, updatable = false, length = 45)
    private String requestIp;

    /** 저장된 리뷰의 값을 복사하므로 이후 원본 수정이나 삭제의 영향을 받지 않습니다. */
    public ReviewHistory(final Review review, final ReviewOperation operation, final LocalDateTime changedAt, final String requestIp) {
        this.reviewId = review.id();
        this.userId = review.userId();
        this.targetType = review.targetType();
        this.targetId = review.targetId();
        this.operation = operation;
        this.content = review.content();
        this.rate = review.rate();
        this.changedAt = changedAt;
        this.requestIp = requestIp;
    }
}
