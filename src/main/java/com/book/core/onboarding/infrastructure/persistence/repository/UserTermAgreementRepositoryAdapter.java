package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.TermAgreementAction;
import com.book.core.onboarding.domain.UserTermAgreement;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    public void saveIfAbsent(final UserTermAgreement agreement) {
        try {
            jpaRepository.save(agreement);
        } catch (final DataIntegrityViolationException exception) {
            if (!jpaRepository.existsByUserIdAndTermIdAndAction(agreement.userId(), agreement.termId(), agreement.action())) {
                throw exception;
            }
        }
    }
}
