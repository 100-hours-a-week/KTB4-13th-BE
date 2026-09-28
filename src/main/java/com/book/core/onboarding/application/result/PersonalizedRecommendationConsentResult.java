package com.book.core.onboarding.application.result;

import java.time.LocalDateTime;

public class PersonalizedRecommendationConsentResult {
    private final boolean consented;
    private final LocalDateTime agreedAt;

    public PersonalizedRecommendationConsentResult(final boolean consented, final LocalDateTime agreedAt) {
        this.consented = consented;
        this.agreedAt = agreedAt;
    }

    public boolean consented() {
        return consented;
    }

    public LocalDateTime agreedAt() {
        return agreedAt;
    }
}
