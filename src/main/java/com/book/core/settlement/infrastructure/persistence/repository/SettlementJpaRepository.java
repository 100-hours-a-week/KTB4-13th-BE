package com.book.core.settlement.infrastructure.persistence.repository;

import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementState;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SettlementJpaRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByState(final SettlementState state);

    @Query("""
        SELECT s.merchantId as merchantId, SUM(s.originalAmount) as amount FROM Settlement s
        WHERE s.merchantId IN :merchantIds AND s.settlementDate >= :startDate AND s.settlementDate <= :endDate
          AND s.state = com.book.core.settlement.domain.SettlementState.SENT
        GROUP BY s.merchantId
        """)
    List<MerchantAmountProjection> sumOriginalAmount(final Collection<Long> merchantIds, final LocalDate startDate,
        final LocalDate endDate);
}
