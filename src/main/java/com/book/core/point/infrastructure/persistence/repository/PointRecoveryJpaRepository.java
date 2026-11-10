package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointRecovery;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRecoveryJpaRepository extends JpaRepository<PointRecovery, Long> {
    Optional<PointRecovery> findBySourceTypeAndSourceReferenceId(PointType sourceType, Long sourceReferenceId);

    List<PointRecovery> findByUserIdAndRemainingAmountGreaterThanOrderByIdAsc(Long userId, BigDecimal amount);
}
