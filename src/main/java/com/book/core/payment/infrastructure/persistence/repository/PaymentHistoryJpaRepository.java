package com.book.core.payment.infrastructure.persistence.repository;

import com.book.core.payment.domain.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentHistoryJpaRepository extends JpaRepository<PaymentHistory, Long> {
}
