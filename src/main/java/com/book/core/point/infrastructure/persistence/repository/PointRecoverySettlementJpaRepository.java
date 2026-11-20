package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointRecoverySettlement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRecoverySettlementJpaRepository extends JpaRepository<PointRecoverySettlement, Long> {
}
