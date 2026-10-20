package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointLotAllocation;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PointLotAllocationJpaRepository extends JpaRepository<PointLotAllocation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT allocation FROM PointLotAllocation allocation
        WHERE allocation.debitHistoryId = :debitHistoryId
            AND allocation.deletedAt IS NULL
        ORDER BY allocation.id ASC
        """)
    List<PointLotAllocation> findByDebitHistoryIdForUpdate(@Param("debitHistoryId") final Long debitHistoryId);
}
