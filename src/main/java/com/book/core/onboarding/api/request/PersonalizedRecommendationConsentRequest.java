package com.book.core.onboarding.api.request;

import jakarta.validation.constraints.NotNull;

public record PersonalizedRecommendationConsentRequest(@NotNull Boolean consented){}
