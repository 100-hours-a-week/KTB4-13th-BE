package com.book.core.payment.application.port;

import com.book.core.payment.domain.Payment;
import java.util.Optional;

public interface PaymentRepositoryPort {
    Payment save(final Payment entity);

    Optional<Payment> findByOrderId(final Long orderId);

    Optional<Payment> findById(final Long paymentId);
}
