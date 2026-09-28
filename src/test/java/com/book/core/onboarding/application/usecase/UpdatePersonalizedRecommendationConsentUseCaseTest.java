package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.onboarding.application.command.UpdatePersonalizedRecommendationConsentCommand;
import com.book.core.onboarding.application.port.UserConsentRepositoryPort;
import com.book.core.onboarding.domain.ConsentType;
import com.book.core.onboarding.domain.UserConsent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdatePersonalizedRecommendationConsentUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final FakeUserConsentRepository consentRepository = new FakeUserConsentRepository();
    private final UpdatePersonalizedRecommendationConsentUseCase useCase =
        new UpdatePersonalizedRecommendationConsentUseCase(consentRepository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void 최초_동의는_새_행을_생성한다() {
        final var result = useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, true));

        assertThat(result.consented()).isTrue();
        assertThat(result.agreedAt()).isNotNull();
        assertThat(consentRepository.all).hasSize(1);
        assertThat(consentRepository.all.get(0).isActive()).isTrue();
    }

    @Test
    void 이미_활성_동의가_있으면_중복_행을_생성하지_않는다() {
        useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, true));
        final var result = useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, true));

        assertThat(result.consented()).isTrue();
        assertThat(consentRepository.all).hasSize(1);
    }

    @Test
    void 철회하면_기존_행이_비활성화된다() {
        useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, true));
        final var result = useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, false));

        assertThat(result.consented()).isFalse();
        assertThat(result.agreedAt()).isNull();
        assertThat(consentRepository.all).hasSize(1);
        assertThat(consentRepository.all.get(0).isActive()).isFalse();
    }

    @Test
    void 활성_동의가_없는_상태에서_철회하면_아무_변화가_없다() {
        final var result = useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, false));

        assertThat(result.consented()).isFalse();
        assertThat(consentRepository.all).isEmpty();
    }

    @Test
    void 철회_후_재동의하면_새_행이_생성되고_기존_행은_철회_상태로_남는다() {
        useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, true));
        useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, false));
        useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, true));

        assertThat(consentRepository.all).hasSize(2);
        assertThat(consentRepository.all.get(0).isActive()).isFalse();
        assertThat(consentRepository.all.get(1).isActive()).isTrue();
    }

    @Test
    void 다른_사용자의_동의는_서로_섞이지_않는다() {
        useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, true));
        useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(99L, true));

        assertThat(consentRepository.findActiveByUserIdAndConsentType(42L, ConsentType.PERSONALIZED_RECOMMENDATION)).isPresent();
        assertThat(consentRepository.findActiveByUserIdAndConsentType(99L, ConsentType.PERSONALIZED_RECOMMENDATION)).isPresent();
        useCase.execute(new UpdatePersonalizedRecommendationConsentCommand(42L, false));
        assertThat(consentRepository.findActiveByUserIdAndConsentType(42L, ConsentType.PERSONALIZED_RECOMMENDATION)).isEmpty();
        assertThat(consentRepository.findActiveByUserIdAndConsentType(99L, ConsentType.PERSONALIZED_RECOMMENDATION)).isPresent();
    }

    private static class FakeUserConsentRepository implements UserConsentRepositoryPort {
        final List<UserConsent> all = new ArrayList<>();
        private long nextId = 1;

        @Override
        public Optional<UserConsent> findActiveByUserIdAndConsentType(final Long userId, final ConsentType consentType) {
            return all.stream()
                .filter(consent -> consent.userId().equals(userId) && consent.consentType() == consentType && consent.isActive())
                .findFirst();
        }

        @Override
        public UserConsent save(final UserConsent userConsent) {
            if (userConsent.id() == null) {
                final UserConsent persisted = new UserConsent(nextId++, userConsent.userId(), userConsent.consentType(),
                    userConsent.policyVersion(), userConsent.agreedAt(), userConsent.withdrawnAt());
                all.add(persisted);
                return persisted;
            }
            return userConsent;
        }
    }
}
