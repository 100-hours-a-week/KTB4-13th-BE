package com.book.core.onboarding.api.response;

import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.domain.OnboardingStatus;
import java.time.LocalDateTime;
import java.util.List;

public record OnboardingProgressResponse(
        OnboardingStatus status,
        LocalDateTime completedAt,
        List<OnboardingAnswerGroupResponse> answers,
        List<Long> bookIds) {
    public static OnboardingProgressResponse from(final OnboardingProgressResult result) {
        return new OnboardingProgressResponse(
                result.status(),
                result.completedAt(),
                result.answers().stream()
                        .map(OnboardingAnswerGroupResponse::from)
                        .toList(),
                result.bookIds());
    }
}
