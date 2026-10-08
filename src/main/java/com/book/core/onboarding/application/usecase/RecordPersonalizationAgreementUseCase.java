package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.RecordPersonalizationAgreementCommand;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermType;
import com.book.core.onboarding.domain.UserTermAgreement;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RecordPersonalizationAgreementUseCase {
    private final TermRepositoryPort termRepository;
    private final UserTermAgreementRepositoryPort agreementRepository;

    public void execute(final RecordPersonalizationAgreementCommand command) {
        final Term term = termRepository.findActiveByTermType(TermType.PERSONALIZED_RECOMMENDATION)
            .orElseThrow(() -> new CoreException(ErrorCode.DEFAULT_ERROR));
        if (agreementRepository.existsAgreedByUserIdAndTermId(command.userId(), term.id())) {
            return;
        }
        agreementRepository.saveIfAbsent(UserTermAgreement.agree(command.userId(), term.id()));
    }
}
