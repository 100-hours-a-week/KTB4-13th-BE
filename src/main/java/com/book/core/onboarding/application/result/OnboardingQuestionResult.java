package com.book.core.onboarding.application.result;

import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.OnboardingQuestion;
import java.util.List;

public class OnboardingQuestionResult {
    private final Long questionId;
    private final String code;
    private final String content;
    private final int minSelection;
    private final Integer maxSelection;
    private final List<OnboardingOptionResult> options;
    private final Long nextQuestionId;

    public OnboardingQuestionResult(final Long questionId, final String code, final String content, final int minSelection,
        final Integer maxSelection, final List<OnboardingOptionResult> options, final Long nextQuestionId) {
        this.questionId = questionId;
        this.code = code;
        this.content = content;
        this.minSelection = minSelection;
        this.maxSelection = maxSelection;
        this.options = options;
        this.nextQuestionId = nextQuestionId;
    }

    public static OnboardingQuestionResult of(final OnboardingQuestion question, final List<OnboardingOption> options,
        final Long nextQuestionId) {
        return new OnboardingQuestionResult(question.id(), question.code(), question.content(), question.minSelection(),
            question.maxSelection(), options.stream().map(OnboardingOptionResult::from).toList(), nextQuestionId);
    }

    public Long questionId() {
        return questionId;
    }

    public String code() {
        return code;
    }

    public String content() {
        return content;
    }

    public int minSelection() {
        return minSelection;
    }

    public Integer maxSelection() {
        return maxSelection;
    }

    public List<OnboardingOptionResult> options() {
        return options;
    }

    public Long nextQuestionId() {
        return nextQuestionId;
    }
}
