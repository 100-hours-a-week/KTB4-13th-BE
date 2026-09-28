package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.onboarding.application.command.GetPersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.port.UserConsentRepositoryPort;
import com.book.core.onboarding.domain.ConsentType;
import com.book.core.onboarding.domain.UserConsent;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetPersonalizedRecommendationConsentUseCaseTest {
    private final FakeUserConsentRepository consentRepository = new FakeUserConsentRepository();
    private final GetPersonalizedRecommendationConsentUseCase useCase = new GetPersonalizedRecommendationConsentUseCase(consentRepository);

    @Test
    void 활성_동의가_있으면_consented_true와_agreedAt을_반환한다() {
        final LocalDateTime agreedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        consentRepository.active.put(42L, UserConsent.create(42L, ConsentType.PERSONALIZED_RECOMMENDATION, "v1", agreedAt));

        final var result = useCase.execute(new GetPersonalizedRecommendationConsentCommand(42L));

        assertThat(result.consented()).isTrue();
        assertThat(result.agreedAt()).isEqualTo(agreedAt);
    }

    @Test
    void 활성_동의가_없으면_consented_false와_agreedAt_null을_반환한다() {
        final var result = useCase.execute(new GetPersonalizedRecommendationConsentCommand(42L));

        assertThat(result.consented()).isFalse();
        assertThat(result.agreedAt()).isNull();
    }

    private static class FakeUserConsentRepository implements UserConsentRepositoryPort {
        final Map<Long, UserConsent> active = new HashMap<>();

        @Override
        public Optional<UserConsent> findActiveByUserIdAndConsentType(final Long userId, final ConsentType consentType) {
            return Optional.ofNullable(active.get(userId));
        }

        @Override
        public UserConsent save(final UserConsent userConsent) {
            if (userConsent.isActive()) {
                active.put(userConsent.userId(), userConsent);
            } else {
                active.remove(userConsent.userId());
            }
            return userConsent;
        }
    }
}
