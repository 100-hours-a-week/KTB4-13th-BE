package com.book.core.recommendation.application.port;

import com.book.core.recommendation.domain.RecommendationCard;
import java.util.List;
import java.util.Optional;

public interface RecommendationCardRepositoryPort {
    RecommendationCard save(final RecommendationCard card);

    List<RecommendationCard> saveAll(final List<RecommendationCard> cards);

    Optional<RecommendationCard> findById(final Long id);
}
