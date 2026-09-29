package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.onboarding.application.command.RecordPersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.application.result.PersonalizedRecommendationConsentResult;
import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermAgreementAction;
import com.book.core.onboarding.domain.TermType;
import com.book.core.onboarding.domain.UserTermAgreement;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * V1 개인화 추천은 철회가 없는 선택 동의다. 동의 행위를 활성 약관에 대한 AGREE 이력으로 한 번만 기록하고, 이미 기록돼 있으면 기존 동의 시각을 반환한다.
 */
@UseCase
@RequiredArgsConstructor
public class RecordPersonalizedRecommendationConsentUseCase {
    private final TermRepositoryPort termRepository;
    private final UserTermAgreementRepositoryPort agreementRepository;
    private final Clock clock;

    @Transactional
    public PersonalizedRecommendationConsentResult execute(final RecordPersonalizedRecommendationConsentCommand command) {
        final Term term = termRepository.findActiveByTermType(TermType.PERSONALIZED_RECOMMENDATION)
            .orElseThrow(() -> new CoreException(ErrorCode.PERSONALIZED_RECOMMENDATION_TERM_NOT_FOUND));
        final UserTermAgreement agreement =
            agreementRepository.findByUserIdAndTermIdAndAction(command.userId(), term.id(), TermAgreementAction.AGREE)
                .orElseGet(() -> agreementRepository.save(UserTermAgreement.agree(command.userId(), term.id(), LocalDateTime.now(clock))));
        return new PersonalizedRecommendationConsentResult(true, agreement.agreedAt());
    }
}
