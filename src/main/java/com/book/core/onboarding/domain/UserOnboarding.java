package com.book.core.onboarding.domain;

import com.book.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "user_onboardings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class UserOnboarding extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 20)
    private OnboardingStatus onboardingStatus;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private UserOnboarding(
            final Long id, final Long userId, final OnboardingStatus onboardingStatus, final LocalDateTime completedAt) {
        super(id);
        this.userId = userId;
        this.onboardingStatus = onboardingStatus;
        this.completedAt = completedAt;
    }

    public static UserOnboarding start(final Long userId) {
        return new UserOnboarding(null, userId, OnboardingStatus.IN_PROGRESS, null);
    }

    public void complete(final LocalDateTime now) {
        if (completedAt == null) {
            this.completedAt = now;
        }
        this.onboardingStatus = OnboardingStatus.COMPLETED;
    }
}
