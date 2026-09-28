package com.book.core.onboarding.api.response;

import com.book.core.onboarding.application.result.OnboardingOptionResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OnboardingOptionResponse {
    private final Long optionId;
    private final Long parentOptionId;
    private final String code;
    private final String content;

    public OnboardingOptionResponse(final Long optionId, final Long parentOptionId, final String code, final String content) {
        this.optionId = optionId;
        this.parentOptionId = parentOptionId;
        this.code = code;
        this.content = content;
    }

    public static OnboardingOptionResponse from(final OnboardingOptionResult result) {
        return new OnboardingOptionResponse(result.optionId(), result.parentOptionId(), result.code(), result.content());
    }

    @JsonProperty
    public Long optionId() {
        return optionId;
    }

    @JsonProperty
    public Long parentOptionId() {
        return parentOptionId;
    }

    @JsonProperty
    public String code() {
        return code;
    }

    @JsonProperty
    public String content() {
        return content;
    }
}
