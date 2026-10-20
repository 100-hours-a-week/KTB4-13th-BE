package com.book.core.payment.application.port;

import com.book.common.paging.SliceResult;
import com.book.core.payment.domain.Payment;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepositoryPort {
    Payment save(final Payment entity);

    Optional<Payment> findByOrderId(final Long orderId);

    Optional<Payment> findByOrderKey(final String orderKey);

    Optional<Payment> findById(final Long paymentId);

    SliceResult<Payment> findForSettlement(final LocalDateTime from, final LocalDateTime to, final int page, final int size);
}
