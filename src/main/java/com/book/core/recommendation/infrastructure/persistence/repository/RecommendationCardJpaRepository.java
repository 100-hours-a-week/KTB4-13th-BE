package com.book.core.recommendation.infrastructure.persistence.repository;

import com.book.core.recommendation.domain.RecommendationCard;
import org.springframework.data.jpa.repository.JpaRepository;

interface RecommendationCardJpaRepository extends JpaRepository<RecommendationCard, Long> {
}
