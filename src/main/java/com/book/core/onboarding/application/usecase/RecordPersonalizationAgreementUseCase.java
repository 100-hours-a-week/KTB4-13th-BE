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

/**
 * 활성 개인화 약관에 대한 사용자의 동의를 한 번만 기록한다. 이미 동의했으면 저장하지 않고 성공한다.
 *
 * 외부 트랜잭션을 두지 않는다. 저장이 자체 트랜잭션에서 실행되어야 동시 요청의 UNIQUE 충돌이 이 흐름 전체를 rollback-only로 만들지 않고
 * {@link UserTermAgreementRepositoryPort#saveIfAbsent}에서 흡수된다.
 */
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
