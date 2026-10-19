package com.book.core.cancel.infrastructure.persistence.repository;

import com.book.core.cancel.domain.Cancel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CancelJpaRepository extends JpaRepository<Cancel, Long> {
    long countByOrderId(final Long orderId);

    List<Cancel> findByOrderIdAndDeletedAtIsNullOrderByCanceledAtDescIdDesc(final Long orderId);
}
