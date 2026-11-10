package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.port.ProfileHistoryRepositoryPort;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.application.result.RecommendationHistory;
import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermType;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetRecommendationHistoryUseCase {
    private final TermRepositoryPort terms;
    private final UserTermAgreementRepositoryPort agreements;
    private final ProfileHistoryRepositoryPort history;

    @Transactional(readOnly = true)
    public RecommendationHistory execute(final Long userId) {
        final boolean consented = terms.findActiveByTermType(TermType.PERSONALIZED_RECOMMENDATION)
            .map((final Term term) -> agreements.existsAgreedByUserIdAndTermId(userId, term.id())).orElse(false);
        if (!consented) {
            return RecommendationHistory.NOT_CONSENTED;
        }
        return new RecommendationHistory(true, history.findOwnedBooks(userId));
    }
}
