package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointRecovery;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface PointRecoveryJpaRepository extends JpaRepository<PointRecovery, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PointRecovery> findBySourceTypeAndSourceReferenceId(PointType sourceType, Long sourceReferenceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PointRecovery> findByUserIdAndRemainingAmountGreaterThanOrderByIdAsc(Long userId, BigDecimal amount);
}
