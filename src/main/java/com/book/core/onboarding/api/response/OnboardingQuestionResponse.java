package com.book.core.onboarding.api.response;

import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import java.util.List;

public record OnboardingQuestionResponse(
        Long questionId,
        String content,
        int minSelection,
        Integer maxSelection,
        List<OnboardingOptionResponse> options,
        Long nextQuestionId) {
    public static OnboardingQuestionResponse from(final OnboardingQuestionResult result) {
        return new OnboardingQuestionResponse(
                result.questionId(),
                result.content(),
                result.minSelection(),
                result.maxSelection(),
                result.options().stream().map(OnboardingOptionResponse::from).toList(),
                result.nextQuestionId());
    }
}
