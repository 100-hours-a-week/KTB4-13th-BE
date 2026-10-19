package com.book.core.cancel.application.port;

import com.book.core.cancel.domain.Cancel;
import java.util.List;

public interface CancelRepositoryPort {
    Cancel save(final Cancel cancel);

    long countByOrderId(final Long orderId);

    List<Cancel> findActiveByOrderIdLatestFirst(final Long orderId);
}
