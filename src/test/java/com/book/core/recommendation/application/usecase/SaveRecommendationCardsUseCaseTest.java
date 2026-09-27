package com.book.core.recommendation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.recommendation.application.port.RecommendationCardRepositoryPort;
import com.book.core.recommendation.application.result.ResolvedRecommendationCard;
import com.book.core.recommendation.domain.RecommendationCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SaveRecommendationCardsUseCaseTest {
    private final FakeRecommendationCardRepository repository = new FakeRecommendationCardRepository();
    private final SaveRecommendationCardsUseCase useCase = new SaveRecommendationCardsUseCase(repository);

    @Test
    void 사용자와_카드_목록으로_RecommendationCard를_생성해_저장한다() {
        final var resolved = List.of(new ResolvedRecommendationCard(1L, "제목1", "작가1", null, "이유1"),
            new ResolvedRecommendationCard(2L, "제목2", "작가2", null, "이유2"));

        final List<RecommendationCard> saved = useCase.execute(42L, resolved);

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).userId()).isEqualTo(42L);
        assertThat(saved.get(0).bookId()).isEqualTo(1L);
        assertThat(saved.get(0).reasonLong()).isEqualTo("이유1");
        assertThat(saved.get(0).id()).isNotNull();
        assertThat(saved.get(1).bookId()).isEqualTo(2L);
    }

    private static class FakeRecommendationCardRepository implements RecommendationCardRepositoryPort {
        private long nextId = 1;

        @Override
        public RecommendationCard save(final RecommendationCard card) {
            return saveAll(List.of(card)).getFirst();
        }

        @Override
        public List<RecommendationCard> saveAll(final List<RecommendationCard> cards) {
            final List<RecommendationCard> result = new ArrayList<>();
            for (final RecommendationCard card : cards) {
                result.add(RecommendationCard.restore(nextId++, card.userId(), card.bookId(), card.reasonLong(), null));
            }
            return result;
        }

        @Override
        public Optional<RecommendationCard> findById(final Long id) {
            return Optional.empty();
        }
    }
}
