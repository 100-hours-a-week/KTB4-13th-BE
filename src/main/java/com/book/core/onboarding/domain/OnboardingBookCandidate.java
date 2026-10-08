package com.book.core.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "onboarding_book_candidates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class OnboardingBookCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "subcategory_code", length = 30)
    private String subcategoryCode;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public OnboardingBookCandidate(final Long id, final Long bookId, final int displayOrder) {
        this(id, bookId, null, displayOrder);
    }

    public OnboardingBookCandidate(final Long id, final Long bookId, final String subcategoryCode, final int displayOrder) {
        this.id = id;
        this.subcategoryCode = subcategoryCode;
        this.bookId = bookId;
        this.displayOrder = displayOrder;
    }
}
