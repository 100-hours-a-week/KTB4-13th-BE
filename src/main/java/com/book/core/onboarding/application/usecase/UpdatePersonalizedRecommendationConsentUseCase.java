package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.command.UpdatePersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.port.UserConsentRepositoryPort;
import com.book.core.onboarding.application.result.PersonalizedRecommendationConsentResult;
import com.book.core.onboarding.domain.ConsentType;
import com.book.core.onboarding.domain.UserConsent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdatePersonalizedRecommendationConsentUseCase {
    private static final String CURRENT_POLICY_VERSION = "v1";

    private final UserConsentRepositoryPort userConsentRepository;
    private final Clock clock;

    @Transactional
    public PersonalizedRecommendationConsentResult execute(final UpdatePersonalizedRecommendationConsentCommand command) {
        final Optional<UserConsent> active =
            userConsentRepository.findActiveByUserIdAndConsentType(command.userId(), ConsentType.PERSONALIZED_RECOMMENDATION);

        if (command.consented()) {
            if (active.isPresent()) {
                return new PersonalizedRecommendationConsentResult(true, active.get().agreedAt());
            }
            final LocalDateTime now = LocalDateTime.now(clock);
            final UserConsent created = userConsentRepository
                .save(UserConsent.create(command.userId(), ConsentType.PERSONALIZED_RECOMMENDATION, CURRENT_POLICY_VERSION, now));
            return new PersonalizedRecommendationConsentResult(true, created.agreedAt());
        }

        if (active.isPresent()) {
            final UserConsent consent = active.get();
            consent.withdraw(LocalDateTime.now(clock));
            userConsentRepository.save(consent);
        }
        return new PersonalizedRecommendationConsentResult(false, null);
    }
}
