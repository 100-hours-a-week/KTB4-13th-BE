package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.TermAgreementAction;
import com.book.core.onboarding.domain.UserTermAgreement;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserTermAgreementRepositoryAdapter implements UserTermAgreementRepositoryPort {
    private final UserTermAgreementJpaRepository jpaRepository;

    @Override
    public Optional<UserTermAgreement> findByUserIdAndTermIdAndAction(final Long userId, final Long termId,
        final TermAgreementAction action) {
        return jpaRepository.findByUserIdAndTermIdAndAction(userId, termId, action);
    }

    @Override
    public UserTermAgreement save(final UserTermAgreement agreement) {
        return jpaRepository.save(agreement);
    }
}
