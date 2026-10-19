package com.book.core.review.infrastructure.persistence.repository;

import com.book.core.review.application.port.ReviewHistoryRepositoryPort;
import com.book.core.review.domain.ReviewHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewHistoryRepositoryAdapter implements ReviewHistoryRepositoryPort {
    private final ReviewHistoryJpaRepository jpaRepository;

    @Override
    public void append(final ReviewHistory history) {
        jpaRepository.save(history);
    }
}
