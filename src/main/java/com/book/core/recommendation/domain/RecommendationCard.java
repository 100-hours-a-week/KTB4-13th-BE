package com.book.core.recommendation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "recommendation_cards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class RecommendationCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "reason_long", nullable = false, columnDefinition = "TEXT")
    private String reasonLong;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RecommendationCard(final Long id, final Long userId, final Long bookId, final String reasonLong, final LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.reasonLong = reasonLong;
        this.createdAt = createdAt;
    }

    public static RecommendationCard create(final Long userId, final Long bookId, final String reasonLong) {
        return new RecommendationCard(null, userId, bookId, reasonLong, null);
    }

    public static RecommendationCard restore(final Long id, final Long userId, final Long bookId, final String reasonLong,
        final LocalDateTime createdAt) {
        return new RecommendationCard(id, userId, bookId, reasonLong, createdAt);
    }

    public boolean isOwnedBy(final Long userId) {
        return this.userId.equals(userId);
    }
}
