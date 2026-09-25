package com.book.core.onboarding.application.result;

import com.book.core.onboarding.domain.OnboardingStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OnboardingProgressResult(
        OnboardingStatus status,
        LocalDateTime completedAt,
        List<OnboardingAnswerGroupResult> answers,
        List<Long> bookIds) {}
