package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.command.GetPersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.port.UserConsentRepositoryPort;
import com.book.core.onboarding.application.result.PersonalizedRecommendationConsentResult;
import com.book.core.onboarding.domain.ConsentType;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetPersonalizedRecommendationConsentUseCase {
    private final UserConsentRepositoryPort userConsentRepository;

    @Transactional(readOnly = true)
    public PersonalizedRecommendationConsentResult execute(final GetPersonalizedRecommendationConsentCommand command) {
        return userConsentRepository.findActiveByUserIdAndConsentType(command.userId(), ConsentType.PERSONALIZED_RECOMMENDATION)
            .map((final var consent) -> new PersonalizedRecommendationConsentResult(true, consent.agreedAt()))
            .orElseGet(() -> new PersonalizedRecommendationConsentResult(false, null));
    }
}
