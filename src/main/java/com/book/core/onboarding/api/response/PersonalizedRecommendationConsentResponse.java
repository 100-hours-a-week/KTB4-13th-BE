package com.book.core.onboarding.api.response;

import com.book.core.onboarding.application.result.PersonalizedRecommendationConsentResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class PersonalizedRecommendationConsentResponse {
    private final boolean consented;
    private final LocalDateTime agreedAt;

    public PersonalizedRecommendationConsentResponse(final boolean consented, final LocalDateTime agreedAt) {
        this.consented = consented;
        this.agreedAt = agreedAt;
    }

    public static PersonalizedRecommendationConsentResponse from(final PersonalizedRecommendationConsentResult result) {
        return new PersonalizedRecommendationConsentResponse(result.consented(), result.agreedAt());
    }

    @JsonProperty
    public boolean consented() {
        return consented;
    }

    @JsonProperty
    public LocalDateTime agreedAt() {
        return agreedAt;
    }
}
