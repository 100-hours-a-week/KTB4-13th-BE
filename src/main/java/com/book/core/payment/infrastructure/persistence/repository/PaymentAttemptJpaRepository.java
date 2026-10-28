package com.book.core.payment.infrastructure.persistence.repository;

import com.book.core.payment.domain.PaymentAttempt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttempt, Long> {
    Optional<PaymentAttempt> findByPgOrderId(String pgOrderId);

    Optional<PaymentAttempt> findByRequestHash(String requestHash);

    Optional<PaymentAttempt> findFirstByPaymentIdOrderByIdDesc(Long paymentId);
}
