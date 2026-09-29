package com.book.core.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_term_agreement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class UserTermAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "term_id", nullable = false)
    private Long termId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TermAgreementAction action;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private UserTermAgreement(final Long userId, final Long termId, final TermAgreementAction action, final LocalDateTime agreedAt) {
        this.userId = userId;
        this.termId = termId;
        this.action = action;
        this.agreedAt = agreedAt;
    }

    public static UserTermAgreement agree(final Long userId, final Long termId) {
        return new UserTermAgreement(userId, termId, TermAgreementAction.AGREE, LocalDateTime.now());
    }
}
