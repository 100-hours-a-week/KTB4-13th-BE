package com.book.core.payment.application.port;

import com.book.core.payment.domain.PgOperation;
import java.util.Optional;

public interface PgOperationRepositoryPort {
    Optional<PgOperation> findByRequestHash(String requestHash);

    boolean existsPending(String orderKey);

    PgOperation save(PgOperation operation);
}
