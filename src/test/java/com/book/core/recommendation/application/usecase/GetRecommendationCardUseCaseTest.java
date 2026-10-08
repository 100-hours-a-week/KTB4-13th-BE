package com.book.core.recommendation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.recommendation.application.command.GetRecommendationCardCommand;
import com.book.core.recommendation.application.port.RecommendationCardRepositoryPort;
import com.book.core.recommendation.application.result.RecommendationCardDetailResult;
import com.book.core.recommendation.domain.RecommendationCard;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetRecommendationCardUseCaseTest {
    private final FakeRecommendationCardRepository cardRepository = new FakeRecommendationCardRepository();
    private final GetRecommendationCardUseCase useCase = new GetRecommendationCardUseCase(cardRepository);

    @Test
    void 소유자의_카드에서_ID_도서_ID와_상세_추천_이유를_반환한다() {
        cardRepository.cards.put(1L, RecommendationCard.restore(1L, 42L, 10L, "긴 추천 이유", null));

        final var result = useCase.execute(new GetRecommendationCardCommand(42L, 1L));

        assertThat(result).isEqualTo(new RecommendationCardDetailResult(1L, 10L, "긴 추천 이유"));
    }

    @Test
    void 존재하지_않는_카드는_예외를_던진다() {
        assertThatThrownBy(() -> useCase.execute(new GetRecommendationCardCommand(42L, 999L))).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.RECOMMENDATION_CARD_NOT_FOUND));
    }

    @Test
    void 다른_사용자의_카드는_존재하지_않는_카드와_동일하게_거부한다() {
        cardRepository.cards.put(1L, RecommendationCard.restore(1L, 42L, 10L, "긴 추천 이유", null));

        assertThatThrownBy(() -> useCase.execute(new GetRecommendationCardCommand(99L, 1L))).isInstanceOf(CoreException.class)
            .satisfies(exception -> assertThat(((CoreException) exception).errorCode()).isEqualTo(ErrorCode.RECOMMENDATION_CARD_NOT_FOUND));
    }

    private static class FakeRecommendationCardRepository implements RecommendationCardRepositoryPort {
        private final Map<Long, RecommendationCard> cards = new HashMap<>();

        @Override
        public RecommendationCard save(final RecommendationCard card) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RecommendationCard> saveAll(final List<RecommendationCard> cards) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RecommendationCard> findById(final Long id) {
            return Optional.ofNullable(cards.get(id));
        }
    }
}
