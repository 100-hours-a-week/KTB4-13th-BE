package com.book.core.onboarding.api.converter;

import com.book.core.onboarding.api.response.OnboardingBookCandidateResponse;
import com.book.core.onboarding.api.response.OnboardingBookCandidatesResponse;
import com.book.core.onboarding.api.response.OnboardingProgressResponse;
import com.book.core.onboarding.api.response.OnboardingQuestionResponse;
import com.book.core.onboarding.api.response.PersonalizedRecommendationConsentResponse;
import com.book.core.onboarding.application.result.OnboardingBookCandidateResult;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.result.OnboardingQuestionResult;
import com.book.core.onboarding.application.result.PersonalizedRecommendationConsentResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OnboardingResultConverter {
    public OnboardingQuestionResponse toOnboardingQuestionResponse(final OnboardingQuestionResult result) {
        return OnboardingQuestionResponse.from(result);
    }

    public OnboardingProgressResponse toOnboardingProgressResponse(final OnboardingProgressResult result) {
        return OnboardingProgressResponse.from(result);
    }

    public OnboardingBookCandidatesResponse toOnboardingBookCandidatesResponse(final List<OnboardingBookCandidateResult> results) {
        return new OnboardingBookCandidatesResponse(results.stream().map(OnboardingBookCandidateResponse::from).toList());
    }

    public PersonalizedRecommendationConsentResponse toPersonalizedRecommendationConsentResponse(
        final PersonalizedRecommendationConsentResult result) {
        return PersonalizedRecommendationConsentResponse.from(result);
    }
}
