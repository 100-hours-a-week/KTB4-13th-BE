package com.book.core.cancel.infrastructure.persistence.repository;

import com.book.core.cancel.domain.Cancel;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

interface CancelJpaRepository extends JpaRepository<Cancel, Long> {
    long countByOrderId(final Long orderId);

    Slice<Cancel> findAllByCanceledAtBetween(final LocalDateTime from, final LocalDateTime to, final Pageable pageable);
}
