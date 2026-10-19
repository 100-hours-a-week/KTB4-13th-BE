package com.book.core.onboarding.application.result;

import java.util.List;

public record OnboardingAnswerGroupResult(Long questionId, List<OnboardingOptionResult> options) {
    public List<Long> optionIds() {
        return options.stream().map(OnboardingOptionResult::optionId).toList();
    }
}
