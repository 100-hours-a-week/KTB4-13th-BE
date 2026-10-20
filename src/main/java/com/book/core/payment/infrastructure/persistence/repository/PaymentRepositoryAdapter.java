package com.book.core.payment.infrastructure.persistence.repository;

import com.book.common.paging.SliceResult;
import com.book.core.payment.application.port.PaymentRepositoryPort;
import com.book.core.payment.domain.Payment;
import com.book.core.payment.domain.enums.PaymentState;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Override
    public SliceResult<Payment> findForSettlement(final LocalDateTime from, final LocalDateTime to, final int page, final int size) {
        final var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        final var slice = jpaRepository.findAllByStateAndPaidAtBetween(PaymentState.SUCCESS, from, to, pageable);
        return new SliceResult<>(slice.getContent(), slice.hasNext());
    }
}
