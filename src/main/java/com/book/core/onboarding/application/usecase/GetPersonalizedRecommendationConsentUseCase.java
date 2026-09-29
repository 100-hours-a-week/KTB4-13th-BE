package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.command.GetPersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.application.result.PersonalizedRecommendationConsentResult;
import com.book.core.onboarding.domain.TermAgreementAction;
import com.book.core.onboarding.domain.TermType;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetPersonalizedRecommendationConsentUseCase {
    private final TermRepositoryPort termRepository;
    private final UserTermAgreementRepositoryPort agreementRepository;

    @Transactional(readOnly = true)
    public PersonalizedRecommendationConsentResult execute(final GetPersonalizedRecommendationConsentCommand command) {
        return termRepository.findActiveByTermType(TermType.PERSONALIZED_RECOMMENDATION).flatMap(
            (final var term) -> agreementRepository.findByUserIdAndTermIdAndAction(command.userId(), term.id(), TermAgreementAction.AGREE))
            .map((final var agreement) -> new PersonalizedRecommendationConsentResult(true, agreement.agreedAt()))
            .orElseGet(() -> new PersonalizedRecommendationConsentResult(false, null));
    }
}
