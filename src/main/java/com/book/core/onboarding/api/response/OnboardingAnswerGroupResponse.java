package com.book.core.onboarding.api.response;

import com.book.core.onboarding.application.result.OnboardingAnswerGroupResult;
import java.util.List;

public record OnboardingAnswerGroupResponse(Long questionId, List<Long> optionIds) {
    public static OnboardingAnswerGroupResponse from(final OnboardingAnswerGroupResult result) {
        return new OnboardingAnswerGroupResponse(result.questionId(), result.optionIds());
    }
}
