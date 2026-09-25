package com.book.core.onboarding.application.result;

import com.book.core.onboarding.domain.OnboardingOption;

public record OnboardingOptionResult(Long optionId, String code, String content) {
    public static OnboardingOptionResult from(final OnboardingOption option) {
        return new OnboardingOptionResult(option.id(), option.code(), option.content());
    }
}
