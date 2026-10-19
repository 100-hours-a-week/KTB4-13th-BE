package com.book.core.cancel.application.port;

import com.book.core.cancel.domain.CancelBalance;
import java.util.Optional;

public interface CancelBalanceRepositoryPort {
    Optional<CancelBalance> findByOrderId(final Long orderId);

    CancelBalance save(final CancelBalance entity);
}
