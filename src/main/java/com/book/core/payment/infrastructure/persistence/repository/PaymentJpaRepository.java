package com.book.core.payment.infrastructure.persistence.repository;

import com.book.core.payment.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(final Long orderId);
}
