package com.book.core.onboarding.application.result;

import com.book.core.onboarding.domain.OnboardingStatus;
import java.time.LocalDateTime;
import java.util.List;

public class OnboardingProgressResult {
    private final OnboardingStatus status;
    private final LocalDateTime completedAt;
    private final List<OnboardingAnswerGroupResult> answers;
    private final List<Long> bookIds;

    public OnboardingProgressResult(final OnboardingStatus status, final LocalDateTime completedAt,
        final List<OnboardingAnswerGroupResult> answers, final List<Long> bookIds) {
        this.status = status;
        this.completedAt = completedAt;
        this.answers = answers;
        this.bookIds = bookIds;
    }

    public OnboardingStatus status() {
        return status;
    }

    public LocalDateTime completedAt() {
        return completedAt;
    }

    public List<OnboardingAnswerGroupResult> answers() {
        return answers;
    }

    public List<Long> bookIds() {
        return bookIds;
    }
}
