package com.book.core.payment.infrastructure.persistence.repository;

import com.book.core.payment.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaymentJpaRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(final Long orderId);

    @Query("""
        SELECT payment FROM Payment payment
        JOIN Order orders ON orders.id = payment.orderId
        WHERE orders.key = :orderKey AND orders.deletedAt IS NULL
        """)
    Optional<Payment> findByOrderKey(@Param("orderKey") final String orderKey);
}
