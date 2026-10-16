package com.book.core.settlement.infrastructure.persistence.repository;

import com.book.core.settlement.domain.SettlementTarget;
import com.book.core.settlement.domain.SettlementTargetSummary;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SettlementTargetJpaRepository extends JpaRepository<SettlementTarget, Long> {
    @Query("""
        SELECT new com.book.core.settlement.domain.SettlementTargetSummary(
          s.merchantId, s.settlementDate, SUM(s.targetAmount), COUNT(s.id), COUNT(DISTINCT s.orderId))
        FROM SettlementTarget s WHERE s.settlementDate = :settlementDate
        GROUP BY s.merchantId, s.settlementDate
        """)
    List<SettlementTargetSummary> findSummary(final LocalDate settlementDate);
}
