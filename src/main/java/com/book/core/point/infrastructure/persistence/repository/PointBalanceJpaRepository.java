package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointBalanceJpaRepository extends JpaRepository<PointBalance, Long> {
    Optional<PointBalance> findByUserId(final Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select balance from PointBalance balance where balance.userId = :userId")
    Optional<PointBalance> findByUserIdForUpdate(@Param("userId") final Long userId);

    @Modifying
    @Query(value = """
        INSERT INTO point_balance (user_id, balance, created_at, updated_at)
        VALUES (:userId, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
        ON DUPLICATE KEY UPDATE id = id
        """, nativeQuery = true)
    void insertIfAbsent(@Param("userId") final Long userId);
}
