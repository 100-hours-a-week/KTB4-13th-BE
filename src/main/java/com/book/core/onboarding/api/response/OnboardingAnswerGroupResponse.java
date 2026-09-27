package com.book.core.onboarding.api.response;

import com.book.core.onboarding.application.result.OnboardingAnswerGroupResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OnboardingAnswerGroupResponse {
    private final Long questionId;
    private final List<Long> optionIds;

    public OnboardingAnswerGroupResponse(final Long questionId, final List<Long> optionIds) {
        this.questionId = questionId;
        this.optionIds = optionIds;
    }

    public static OnboardingAnswerGroupResponse from(final OnboardingAnswerGroupResult result) {
        return new OnboardingAnswerGroupResponse(result.questionId(), result.optionIds());
    }

    @JsonProperty
    public Long questionId() {
        return questionId;
    }

    @JsonProperty
    public List<Long> optionIds() {
        return optionIds;
    }
}
