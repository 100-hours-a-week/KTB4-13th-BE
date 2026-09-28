package com.book.core.onboarding.api.response;

import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.domain.OnboardingStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class OnboardingProgressResponse {
    private final OnboardingStatus status;
    private final LocalDateTime completedAt;
    private final List<OnboardingAnswerGroupResponse> answers;
    private final List<Long> bookIds;

    public OnboardingProgressResponse(final OnboardingStatus status, final LocalDateTime completedAt,
        final List<OnboardingAnswerGroupResponse> answers, final List<Long> bookIds) {
        this.status = status;
        this.completedAt = completedAt;
        this.answers = answers;
        this.bookIds = bookIds;
    }

    public static OnboardingProgressResponse from(final OnboardingProgressResult result) {
        return new OnboardingProgressResponse(result.status(), result.completedAt(),
            result.answers().stream().map(OnboardingAnswerGroupResponse::from).toList(), result.bookIds());
    }

    @JsonProperty
    public OnboardingStatus status() {
        return status;
    }

    @JsonProperty
    public LocalDateTime completedAt() {
        return completedAt;
    }

    @JsonProperty
    public List<OnboardingAnswerGroupResponse> answers() {
        return answers;
    }

    @JsonProperty
    public List<Long> bookIds() {
        return bookIds;
    }
}
