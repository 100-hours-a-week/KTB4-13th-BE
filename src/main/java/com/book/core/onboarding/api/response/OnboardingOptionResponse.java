package com.book.core.onboarding.api.response;

import com.book.core.onboarding.application.result.OnboardingOptionResult;

public record OnboardingOptionResponse(Long optionId, String code, String content) {
    public static OnboardingOptionResponse from(final OnboardingOptionResult result) {
        return new OnboardingOptionResponse(result.optionId(), result.code(), result.content());
    }
}
