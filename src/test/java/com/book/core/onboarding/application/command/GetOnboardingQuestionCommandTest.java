package com.book.core.onboarding.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetOnboardingQuestionCommandTest {
    @Test
    void 부모_선택지를_생략하면_비어있는_값으로_생성한다() {
        assertThat(new GetOnboardingQuestionCommand(42L, 4L).parentOptionIds()).isEmpty();
        assertThat(new GetOnboardingQuestionCommand(42L, 4L, null)).isEqualTo(new GetOnboardingQuestionCommand(42L, 4L));
    }

    @Test
    void 부모_선택지를_전달하면_방어적으로_복사한다() {
        final List<Long> parentOptionIds = new ArrayList<>(List.of(9L, 10L));
        final var command = new GetOnboardingQuestionCommand(42L, 4L, parentOptionIds);

        parentOptionIds.add(11L);

        assertThat(command.parentOptionIds()).hasValue(List.of(9L, 10L));
        assertThatThrownBy(() -> command.parentOptionIds().orElseThrow().add(12L)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 전달한_부모_선택지가_비었거나_유효하지_않은_ID를_포함하면_예외를_던진다() {
        for (final List<Long> parentOptionIds : List.of(List.<Long>of(), Arrays.asList(9L, null), List.of(0L), List.of(-1L),
            List.of(9L, 9L))) {
            assertThatThrownBy(() -> new GetOnboardingQuestionCommand(42L, 4L, parentOptionIds)).isInstanceOf(CoreException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_REQUEST);
        }
    }
}
