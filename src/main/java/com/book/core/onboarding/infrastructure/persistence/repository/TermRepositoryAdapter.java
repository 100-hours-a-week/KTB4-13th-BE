package com.book.core.onboarding.infrastructure.persistence.repository;

import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class TermRepositoryAdapter implements TermRepositoryPort {
    private final TermJpaRepository jpaRepository;

    @Override
    public Optional<Term> findActiveByTermType(final TermType termType) {
        return jpaRepository.findByTermTypeAndIsActiveTrue(termType);
    }
}
