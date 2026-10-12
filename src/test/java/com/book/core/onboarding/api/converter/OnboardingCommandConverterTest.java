package com.book.core.onboarding.api.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.onboarding.api.request.OnboardingPreferenceAnswerRequest;
import com.book.core.onboarding.api.request.PutOnboardingPreferencesRequest;
import com.book.core.onboarding.application.command.OnboardingPreferenceAnswerCommand;
import com.book.core.onboarding.application.command.PutOnboardingPreferencesCommand;
import java.util.List;
import org.junit.jupiter.api.Test;

class OnboardingCommandConverterTest {
    private final OnboardingCommandConverter converter = new OnboardingCommandConverter();

    @Test
    void 전체_수정_요청을_질문별_답변과_도서_목록을_가진_Command로_변환한다() {
        final var request = new PutOnboardingPreferencesRequest(
            List.of(new OnboardingPreferenceAnswerRequest(1L, List.of(1L, 2L)), new OnboardingPreferenceAnswerRequest(3L, List.of(9L))),
            List.of(100L, 200L));

        final PutOnboardingPreferencesCommand command = converter.toPutOnboardingPreferencesCommand(7L, request);

        assertThat(command).isEqualTo(new PutOnboardingPreferencesCommand(7L,
            List.of(new OnboardingPreferenceAnswerCommand(1L, List.of(1L, 2L)), new OnboardingPreferenceAnswerCommand(3L, List.of(9L))),
            List.of(100L, 200L)));
    }
}
