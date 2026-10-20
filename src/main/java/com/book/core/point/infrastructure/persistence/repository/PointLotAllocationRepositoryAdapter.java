package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.application.port.PointLotAllocationRepositoryPort;
import com.book.core.point.domain.PointLotAllocation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointLotAllocationRepositoryAdapter implements PointLotAllocationRepositoryPort {
    private final PointLotAllocationJpaRepository jpaRepository;

    @Override
    public void saveAll(final List<PointLotAllocation> allocations) {
        jpaRepository.saveAll(allocations);
    }

    @Override
    public List<PointLotAllocation> findByDebitHistoryIdForUpdate(final Long debitHistoryId) {
        return jpaRepository.findByDebitHistoryIdForUpdate(debitHistoryId);
    }
}
