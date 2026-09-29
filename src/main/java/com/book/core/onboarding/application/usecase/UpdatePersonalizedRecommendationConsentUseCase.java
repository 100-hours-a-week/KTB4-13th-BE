package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.UpdatePersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.application.result.PersonalizedRecommendationConsentResult;
import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermAgreementAction;
import com.book.core.onboarding.domain.TermType;
import com.book.core.onboarding.domain.UserTermAgreement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * V1 개인화 추천은 철회가 없는 선택 동의다. 동의하면 활성 약관에 AGREE 이력을 한 번만 남기고, 미동의 요청은 이력을 만들거나 기존 동의를 바꾸지 않은 채 현재 동의
 * 상태를 반환한다.
 */
@UseCase
@RequiredArgsConstructor
public class UpdatePersonalizedRecommendationConsentUseCase {
    private final TermRepositoryPort termRepository;
    private final UserTermAgreementRepositoryPort agreementRepository;
    private final Clock clock;

    @Transactional
    public PersonalizedRecommendationConsentResult execute(final UpdatePersonalizedRecommendationConsentCommand command) {
        final Optional<Term> activeTerm = termRepository.findActiveByTermType(TermType.PERSONALIZED_RECOMMENDATION);

        if (!command.consented()) {
            return activeTerm.flatMap(term -> findAgreement(command.userId(), term))
                .map(agreement -> new PersonalizedRecommendationConsentResult(true, agreement.agreedAt()))
                .orElseGet(() -> new PersonalizedRecommendationConsentResult(false, null));
        }

        final Term term = activeTerm.orElseThrow(() -> new CoreException(ErrorCode.PERSONALIZED_RECOMMENDATION_TERM_NOT_FOUND));
        final UserTermAgreement agreement = findAgreement(command.userId(), term)
            .orElseGet(() -> agreementRepository.save(UserTermAgreement.agree(command.userId(), term.id(), LocalDateTime.now(clock))));
        return new PersonalizedRecommendationConsentResult(true, agreement.agreedAt());
    }

    private Optional<UserTermAgreement> findAgreement(final Long userId, final Term term) {
        return agreementRepository.findByUserIdAndTermIdAndAction(userId, term.id(), TermAgreementAction.AGREE);
    }
}
