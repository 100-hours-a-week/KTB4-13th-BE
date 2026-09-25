package com.book.core.onboarding.application.result;

import com.book.core.onboarding.domain.OnboardingOption;

public class OnboardingOptionResult {
    private final Long optionId;
    private final String code;
    private final String content;

    public OnboardingOptionResult(final Long optionId, final String code, final String content) {
        this.optionId = optionId;
        this.code = code;
        this.content = content;
    }

    public static OnboardingOptionResult from(final OnboardingOption option) {
        return new OnboardingOptionResult(option.id(), option.code(), option.content());
    }

    public Long optionId() {
        return optionId;
    }

    public String code() {
        return code;
    }

    public String content() {
        return content;
    }
}
