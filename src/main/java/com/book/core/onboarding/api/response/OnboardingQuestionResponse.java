package com.book.core.onboarding.api.response;

import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OnboardingQuestionResponse {
    private final Long questionId;
    private final String content;
    private final int minSelection;
    private final Integer maxSelection;
    private final List<OnboardingOptionResponse> options;
    private final Long nextQuestionId;

    public OnboardingQuestionResponse(final Long questionId, final String content, final int minSelection, final Integer maxSelection,
        final List<OnboardingOptionResponse> options, final Long nextQuestionId) {
        this.questionId = questionId;
        this.content = content;
        this.minSelection = minSelection;
        this.maxSelection = maxSelection;
        this.options = options;
        this.nextQuestionId = nextQuestionId;
    }

    public static OnboardingQuestionResponse from(final OnboardingQuestionResult result) {
        return new OnboardingQuestionResponse(result.questionId(), result.content(), result.minSelection(), result.maxSelection(),
            result.options().stream().map(OnboardingOptionResponse::from).toList(), result.nextQuestionId());
    }

    @JsonProperty
    public Long questionId() {
        return questionId;
    }

    @JsonProperty
    public String content() {
        return content;
    }

    @JsonProperty
    public int minSelection() {
        return minSelection;
    }

    @JsonProperty
    public Integer maxSelection() {
        return maxSelection;
    }

    @JsonProperty
    public List<OnboardingOptionResponse> options() {
        return options;
    }

    @JsonProperty
    public Long nextQuestionId() {
        return nextQuestionId;
    }
}
