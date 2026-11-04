package com.book.core.payment.infrastructure.persistence.repository;

import com.book.core.payment.domain.PgOperation;
import java.util.Optional;
import java.util.List;
import com.book.core.payment.domain.PgOperationType;
import org.springframework.data.jpa.repository.JpaRepository;

interface PgOperationJpaRepository extends JpaRepository<PgOperation, Long> {
    Optional<PgOperation> findByRequestHash(String requestHash);

    boolean existsByOrderKeyAndCompletedFalseAndRejectedFalseAndBlocksOrderTrue(String orderKey);

    List<PgOperation> findByTypeAndCompletedFalseAndRejectedFalseAndReviewRequiredFalseOrderByRequestedAtAsc(PgOperationType type);
}
