package com.book.core.user.infrastructure.persistence.repository;

import com.book.core.user.domain.User;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserJpaRepository extends JpaRepository<User, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") final Long id);

    @Query("select u.id from User u where u.deleteRequestedAt <= :requestedBefore and u.deletedAt is null order by u.id")
    List<Long> findWithdrawalTargetIds(@Param("requestedBefore") final LocalDateTime requestedBefore);
}
