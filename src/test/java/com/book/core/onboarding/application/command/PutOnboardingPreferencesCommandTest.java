package com.book.core.onboarding.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PutOnboardingPreferencesCommandTest {
    private static final List<OnboardingPreferenceAnswerCommand> ANSWERS =
        List.of(new OnboardingPreferenceAnswerCommand(1L, List.of(1L)), new OnboardingPreferenceAnswerCommand(2L, List.of(6L)));

    @Test
    void 사용자_ID와_질문별_답변과_도서_목록으로_생성한다() {
        final var command = new PutOnboardingPreferencesCommand(1L, ANSWERS, List.of(10L, 20L));

        assertThat(command.userId()).isEqualTo(1L);
        assertThat(command.answers()).containsExactlyElementsOf(ANSWERS);
        assertThat(command.bookIds()).containsExactly(10L, 20L);
    }

    @Test
    void 빈_도서_목록을_허용한다() {
        assertThat(new PutOnboardingPreferencesCommand(1L, ANSWERS, List.of()).bookIds()).isEmpty();
    }

    @Test
    void 사용자_ID가_없거나_양수가_아니면_예외를_던진다() {
        for (final Long userId : Arrays.asList(null, 0L, -1L)) {
            assertThatThrownBy(() -> new PutOnboardingPreferencesCommand(userId, ANSWERS, List.of())).isInstanceOf(CoreException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }

    @Test
    void 답변_목록이_null이거나_null_답변을_포함하면_예외를_던진다() {
        for (final List<OnboardingPreferenceAnswerCommand> answers : Arrays.asList(null, Arrays.asList(ANSWERS.getFirst(), null))) {
            assertThatThrownBy(() -> new PutOnboardingPreferencesCommand(1L, answers, List.of())).isInstanceOf(CoreException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
    }

    @Test
    void 질문_ID가_중복되면_예외를_던진다() {
        final var answers =
            List.of(new OnboardingPreferenceAnswerCommand(1L, List.of(1L)), new OnboardingPreferenceAnswerCommand(1L, List.of(2L)));

        assertThatThrownBy(() -> new PutOnboardingPreferencesCommand(1L, answers, List.of())).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
    }

    @Test
    void 도서_목록이_null이거나_유효하지_않은_ID를_포함하면_예외를_던진다() {
        for (final List<Long> bookIds : Arrays.asList(null, Arrays.asList(1L, null), List.of(0L), List.of(-1L))) {
            assertThatThrownBy(() -> new PutOnboardingPreferencesCommand(1L, ANSWERS, bookIds)).isInstanceOf(CoreException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ONBOARDING_BOOK_SELECTION);
        }
    }

    @Test
    void 도서_ID가_중복되면_예외를_던진다() {
        assertThatThrownBy(() -> new PutOnboardingPreferencesCommand(1L, ANSWERS, List.of(3L, 3L))).isInstanceOf(CoreException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ONBOARDING_BOOK_SELECTION);
    }

    @Test
    void 답변과_도서_목록을_방어적으로_복사하고_변경을_허용하지_않는다() {
        final List<OnboardingPreferenceAnswerCommand> answers = new ArrayList<>(List.of(ANSWERS.getFirst()));
        final List<Long> bookIds = new ArrayList<>(List.of(5L));
        final var command = new PutOnboardingPreferencesCommand(1L, answers, bookIds);

        answers.add(ANSWERS.getLast());
        bookIds.add(6L);

        assertThat(command.answers()).containsExactly(ANSWERS.getFirst());
        assertThat(command.bookIds()).containsExactly(5L);
        assertThatThrownBy(() -> command.answers().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> command.bookIds().add(7L)).isInstanceOf(UnsupportedOperationException.class);
    }
}
