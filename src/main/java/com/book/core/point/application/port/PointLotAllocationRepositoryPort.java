package com.book.core.point.application.port;

import com.book.core.point.domain.PointLotAllocation;
import java.util.List;

public interface PointLotAllocationRepositoryPort {
    void saveAll(final List<PointLotAllocation> allocations);

    List<PointLotAllocation> findByDebitHistoryIdForUpdate(final Long debitHistoryId);
}
