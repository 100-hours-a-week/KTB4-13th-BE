package com.book.core.cancel.infrastructure.persistence.repository;

import com.book.common.paging.SliceResult;
import com.book.core.cancel.application.port.CancelRepositoryPort;
import com.book.core.cancel.domain.Cancel;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class CancelRepositoryAdapter implements CancelRepositoryPort {
    private final CancelJpaRepository jpaRepository;

    @Override
    public Cancel save(final Cancel cancel) {
        return jpaRepository.save(cancel);
    }

    @Override
    public long countByOrderId(final Long orderId) {
        return jpaRepository.countByOrderId(orderId);
    }

    @Override
    public List<Cancel> findActiveByOrderIdLatestFirst(final Long orderId) {
        return jpaRepository.findByOrderIdAndDeletedAtIsNullOrderByCanceledAtDescIdDesc(orderId);
    }

    @Override
    public SliceResult<Cancel> findForSettlement(final LocalDateTime from, final LocalDateTime to, final int page, final int size) {
        final PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        final Slice<Cancel> slice = jpaRepository.findAllByCanceledAtBetween(from, to, pageable);
        return new SliceResult<>(slice.getContent(), slice.hasNext());
    }
}
