package com.book.core.preference.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.memory.application.result.UserMemoryGroupResult;
import com.book.core.memory.application.usecase.GetUserMemoriesUseCase;
import com.book.core.memory.domain.UserMemoryType;
import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.result.OnboardingAnswerGroupResult;
import com.book.core.onboarding.application.result.OnboardingOptionResult;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.domain.OnboardingStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserPreferenceServiceTest {
    private static final Long USER_ID = 42L;

    private final GetOnboardingProgressUseCase getOnboardingProgressUseCase = mock(GetOnboardingProgressUseCase.class);
    private final GetUserMemoriesUseCase getUserMemoriesUseCase = mock(GetUserMemoriesUseCase.class);
    private final UserPreferenceService userPreferenceService =
        new UserPreferenceService(getOnboardingProgressUseCase, getUserMemoriesUseCase);

    @Test
    void 온보딩을_완료하지_않았으면_취향_기억을_조회하지_않고_409를_던진다() {
        when(getOnboardingProgressUseCase.executeCompleted(new GetOnboardingProgressCommand(USER_ID)))
            .thenThrow(new CoreException(ErrorCode.ONBOARDING_NOT_COMPLETED));

        assertThatThrownBy(() -> userPreferenceService.getPreferences(USER_ID)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.ONBOARDING_NOT_COMPLETED));
        verifyNoInteractions(getUserMemoriesUseCase);
    }

    @Test
    void 완료한_온보딩_답변과_취향_기억을_함께_반환한다() {
        final var answers = List.of(new OnboardingAnswerGroupResult(3L, List.of(new OnboardingOptionResult(9L, null, "economy", "경제경영"))));
        final var memories = List.of(new UserMemoryGroupResult(List.of(102L, 101L), UserMemoryType.TOPIC, "경제 책을 좋아해요", List.of("경제경영"),
            List.of(LocalDate.of(2026, 10, 16))));
        when(getOnboardingProgressUseCase.executeCompleted(new GetOnboardingProgressCommand(USER_ID)))
            .thenReturn(new OnboardingProgressResult(OnboardingStatus.COMPLETED, null, answers, List.of()));
        when(getUserMemoriesUseCase.execute(USER_ID)).thenReturn(memories);

        final var result = userPreferenceService.getPreferences(USER_ID);

        assertThat(result.onboarding()).isEqualTo(answers);
        assertThat(result.memories()).isEqualTo(memories);
    }
}
