package com.book.core.cancel.application.port;

import com.book.common.paging.SliceResult;
import com.book.core.cancel.domain.Cancel;
import java.time.LocalDateTime;
import java.util.List;

public interface CancelRepositoryPort {
    Cancel save(final Cancel cancel);

    long countByOrderId(final Long orderId);

    List<Cancel> findActiveByOrderIdLatestFirst(final Long orderId);

    SliceResult<Cancel> findForSettlement(final LocalDateTime from, final LocalDateTime to, final int page, final int size);
}
