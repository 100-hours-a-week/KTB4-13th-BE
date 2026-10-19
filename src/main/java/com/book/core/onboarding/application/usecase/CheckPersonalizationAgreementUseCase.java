package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermType;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CheckPersonalizationAgreementUseCase {
    private final TermRepositoryPort termRepository;
    private final UserTermAgreementRepositoryPort agreementRepository;

    @Transactional(readOnly = true)
    public boolean execute(final Long userId) {
        return termRepository.findActiveByTermType(TermType.PERSONALIZED_RECOMMENDATION)
            .map((final Term term) -> agreementRepository.existsAgreedByUserIdAndTermId(userId, term.id())).orElse(false);
    }
}
