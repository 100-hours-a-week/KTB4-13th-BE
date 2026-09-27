package com.book.core.recommendation.infrastructure.persistence.repository;

import com.book.core.recommendation.application.port.RecommendationCardRepositoryPort;
import com.book.core.recommendation.domain.RecommendationCard;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class RecommendationCardRepositoryAdapter implements RecommendationCardRepositoryPort {
    private final RecommendationCardJpaRepository jpaRepository;

    @Override
    public RecommendationCard save(final RecommendationCard card) {
        return jpaRepository.save(card);
    }

    @Override
    public List<RecommendationCard> saveAll(final List<RecommendationCard> cards) {
        return jpaRepository.saveAll(cards);
    }

    @Override
    public Optional<RecommendationCard> findById(final Long id) {
        return jpaRepository.findById(id);
    }
}
