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
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 활성 개인화 약관에 대한 사용자의 동의를 한 번만 기록한다. 이미 동의했으면 저장하지 않고 성공한다.
 *
 * 외부 트랜잭션을 두지 않아 저장이 자체 트랜잭션에서 실행된다. 같은 사용자의 동시 요청이 먼저 저장해 UNIQUE 제약에 걸리면 이 저장 트랜잭션만 롤백되므로, 동의가 기록된
 * 것을 다시 확인하고 정상 종료할 수 있다.
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
        try {
            agreementRepository.save(UserTermAgreement.agree(command.userId(), term.id()));
        } catch (final DataIntegrityViolationException exception) {
            if (!agreementRepository.existsAgreedByUserIdAndTermId(command.userId(), term.id())) {
                throw exception;
            }
        }
    }
}
