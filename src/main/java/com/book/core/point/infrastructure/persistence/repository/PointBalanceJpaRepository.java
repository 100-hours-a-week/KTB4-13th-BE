package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointBalanceJpaRepository extends JpaRepository<PointBalance, Long> {
    Optional<PointBalance> findByUserId(final Long userId);
}
