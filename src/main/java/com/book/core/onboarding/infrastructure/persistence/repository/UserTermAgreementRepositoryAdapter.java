package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.TermAgreementAction;
import com.book.core.onboarding.domain.UserTermAgreement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserTermAgreementRepositoryAdapter implements UserTermAgreementRepositoryPort {
    private final UserTermAgreementJpaRepository jpaRepository;

    @Override
    public boolean existsAgreedByUserIdAndTermId(final Long userId, final Long termId) {
        return jpaRepository.existsByUserIdAndTermIdAndAction(userId, termId, TermAgreementAction.AGREE);
    }

    @Override
    public UserTermAgreement save(final UserTermAgreement agreement) {
        return jpaRepository.save(agreement);
    }
}
