package com.book.core.payment.infrastructure.persistence.repository;

import com.book.core.payment.application.port.PgOperationRepositoryPort;
import com.book.core.payment.domain.PgOperation;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PgOperationRepositoryAdapter implements PgOperationRepositoryPort {
    private final PgOperationJpaRepository repository;

    @Override
    public Optional<PgOperation> findByRequestHash(final String hash) {
        return repository.findByRequestHash(hash);
    }

    @Override
    public boolean existsPending(final String orderKey) {
        return repository.existsByOrderKeyAndCompletedFalseAndRejectedFalse(orderKey);
    }

    @Override
    public PgOperation save(final PgOperation operation) {
        return repository.save(operation);
    }
}
