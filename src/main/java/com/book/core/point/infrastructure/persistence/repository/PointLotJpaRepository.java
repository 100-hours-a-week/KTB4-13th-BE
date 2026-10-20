package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointLot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PointLotJpaRepository extends JpaRepository<PointLot, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lot FROM PointLot lot WHERE lot.id = :pointLotId AND lot.deletedAt IS NULL")
    java.util.Optional<PointLot> findByIdForUpdate(@Param("pointLotId") final Long pointLotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT lot FROM PointLot lot
        WHERE lot.userId = :userId
            AND lot.expiresAt > :now
            AND lot.remainingAmount > 0
            AND lot.deletedAt IS NULL
        ORDER BY lot.expiresAt ASC, lot.id ASC
        """)
    List<PointLot> findAvailableByUserIdForUpdate(@Param("userId") final Long userId, @Param("now") final LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT lot FROM PointLot lot
        WHERE lot.expiresAt <= :now
            AND lot.remainingAmount > 0
            AND lot.deletedAt IS NULL
        ORDER BY lot.userId ASC, lot.expiresAt ASC, lot.id ASC
        """)
    List<PointLot> findExpiredForUpdate(@Param("now") final LocalDateTime now, final Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT lot FROM PointLot lot
        WHERE lot.expiresAt > :now
            AND lot.expiresAt <= :expiresBefore
            AND lot.remainingAmount > 0
            AND lot.expiryNotificationSentAt IS NULL
            AND lot.deletedAt IS NULL
        ORDER BY lot.userId ASC, lot.expiresAt ASC, lot.id ASC
        """)
    List<PointLot> findExpiringForUpdate(@Param("now") final LocalDateTime now, @Param("expiresBefore") final LocalDateTime expiresBefore,
        final Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(lot.remainingAmount), 0) FROM PointLot lot
        WHERE lot.userId = :userId
            AND lot.expiresAt > :now
            AND lot.remainingAmount > 0
            AND lot.deletedAt IS NULL
        """)
    BigDecimal sumAvailableByUserId(@Param("userId") final Long userId, @Param("now") final LocalDateTime now);
}
