package com.book.core.onboarding.application.result;

import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.OnboardingQuestion;
import java.util.List;

public record OnboardingQuestionResult(
        Long questionId,
        String code,
        String content,
        int minSelection,
        Integer maxSelection,
        List<OnboardingOptionResult> options,
        Long nextQuestionId) {
    public static OnboardingQuestionResult of(
            final OnboardingQuestion question, final List<OnboardingOption> options, final Long nextQuestionId) {
        return new OnboardingQuestionResult(
                question.id(),
                question.code(),
                question.content(),
                question.minSelection(),
                question.maxSelection(),
                options.stream().map(OnboardingOptionResult::from).toList(),
                nextQuestionId);
    }
}
