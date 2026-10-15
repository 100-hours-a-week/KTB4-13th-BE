package com.book.core.point.infrastructure.persistence.repository;

import com.book.core.point.domain.PointBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PointBalanceJpaRepository extends JpaRepository<PointBalance, Long> {
    Optional<PointBalance> findByUserId(final Long userId);

    boolean existsByUserId(final Long userId);

    @Query(value = """
        SELECT u.id
        FROM users u
        WHERE u.deleted_at IS NULL
          AND u.created_at <= :registeredBefore
          AND u.id > :afterUserId
          AND NOT EXISTS (SELECT 1 FROM point_balance pb WHERE pb.user_id = u.id)
        ORDER BY u.id
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> findActiveUserIdsWithoutPointBalance(@Param("registeredBefore") final LocalDateTime registeredBefore,
        @Param("afterUserId") final Long afterUserId, @Param("limit") final int limit);
}
