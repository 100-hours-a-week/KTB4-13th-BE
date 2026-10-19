package com.book.core.onboarding.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class OnboardingPreferenceAnswerCommandTest {
    @Test
    void 질문_ID와_선택지_목록으로_생성하고_선택_개수는_UseCase에서_검증하므로_빈_목록을_허용한다() {
        final var command = new OnboardingPreferenceAnswerCommand(3L, List.of(9L, 10L));

        assertThat(command.questionId()).isEqualTo(3L);
        assertThat(command.optionIds()).containsExactly(9L, 10L);
        assertThat(new OnboardingPreferenceAnswerCommand(1L, List.of()).optionIds()).isEmpty();
    }

    @Test
    void 질문_ID가_없거나_양수가_아니면_예외를_던진다() {
        for (final Long questionId : Arrays.asList(null, 0L, -1L)) {
            assertThatThrownBy(() -> new OnboardingPreferenceAnswerCommand(questionId, List.of(1L))).isInstanceOf(CoreException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }

    @Test
    void 선택지_목록이_null이거나_유효하지_않거나_중복된_ID를_포함하면_예외를_던진다() {
        for (final List<Long> optionIds : Arrays.asList(null, Arrays.asList(1L, null), List.of(0L), List.of(-1L), List.of(2L, 2L))) {
            assertThatThrownBy(() -> new OnboardingPreferenceAnswerCommand(1L, optionIds)).isInstanceOf(CoreException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
    }

    @Test
    void 선택지_목록을_방어적으로_복사하고_변경을_허용하지_않는다() {
        final List<Long> optionIds = new ArrayList<>(List.of(1L));
        final var command = new OnboardingPreferenceAnswerCommand(1L, optionIds);

        optionIds.add(2L);

        assertThat(command.optionIds()).containsExactly(1L);
        assertThatThrownBy(() -> command.optionIds().add(3L)).isInstanceOf(UnsupportedOperationException.class);
    }
}
