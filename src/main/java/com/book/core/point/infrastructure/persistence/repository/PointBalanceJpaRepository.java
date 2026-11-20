package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PointBalanceJpaRepository extends JpaRepository<PointBalance, Long> {
    Optional<PointBalance> findByUserId(final Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PointBalance> findWithLockByUserId(final Long userId);
}
