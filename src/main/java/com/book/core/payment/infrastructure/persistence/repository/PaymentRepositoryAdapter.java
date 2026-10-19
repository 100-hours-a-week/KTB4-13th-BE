package com.book.core.payment.infrastructure.persistence.repository;

import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PaymentRepositoryAdapter implements PaymentRepositoryPort {
    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(final Payment entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<Payment> findByOrderId(final Long orderId) {
        return jpaRepository.findByOrderId(orderId);
    }

    @Override
    public Optional<Payment> findByOrderKey(final String orderKey) {
        return jpaRepository.findByOrderKey(orderKey);
    }

    @Override
    public Optional<Payment> findById(final Long paymentId) {
        return jpaRepository.findById(paymentId);
    }
}
