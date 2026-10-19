package com.book.core.cancel.infrastructure.persistence.repository;

import com.book.core.cancel.domain.CancelBalance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface CancelBalanceJpaRepository extends JpaRepository<CancelBalance, Long> {
    Optional<CancelBalance> findByOrderId(final Long orderId);
}
