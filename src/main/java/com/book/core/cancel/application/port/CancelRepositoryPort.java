package com.book.core.cancel.application.port;

import com.book.core.cancel.domain.Cancel;

public interface CancelRepositoryPort {
    Cancel save(final Cancel cancel);

    long countByOrderId(final Long orderId);
}
