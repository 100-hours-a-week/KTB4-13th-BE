package com.book.core.onboarding.domain;

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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "onboarding_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class OnboardingQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 40)
    private String content;

    @Column(name = "min_selection", nullable = false)
    private int minSelection;

    @Column(name = "max_selection")
    private Integer maxSelection;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public OnboardingQuestion(
            final Long id,
            final String code,
            final String content,
            final int minSelection,
            final Integer maxSelection,
            final int displayOrder) {
        this.id = id;
        this.code = code;
        this.content = content;
        this.minSelection = minSelection;
        this.maxSelection = maxSelection;
        this.displayOrder = displayOrder;
    }
}
